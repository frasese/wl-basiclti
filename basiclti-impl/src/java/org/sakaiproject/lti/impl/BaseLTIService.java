/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008, 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.lti.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.lti.api.LTIService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.foorm.SakaiFoorm;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Implements the LTIService, all but a Storage model.
 * </p>
 */
public abstract class BaseLTIService implements LTIService {
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(BaseLTIService.class);

	/** Constants */
	private final String ADMIN_SITE = "!admin";
	public final String LAUNCH_PREFIX = "/access/basiclti/site/";

	/** Resource bundle using current language locale */
	protected static ResourceLoader rb = new ResourceLoader("ltiservice");
	/**
	 * 
	 */
	protected static SakaiFoorm foorm = new SakaiFoorm();

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *          The SessionManager.
	 */
	public void setSessionManager(SessionManager service) {
		m_sessionManager = service;
	}

	/** Dependency: UsageSessionService */
	protected UsageSessionService m_usageSessionService = null;

	/**
	 * Dependency: UsageSessionService.
	 * 
	 * @param service
	 *          The UsageSessionService.
	 */
	public void setUsageSessionService(UsageSessionService service) {
		m_usageSessionService = service;
	}

	/** Dependency: UserDirectoryService */
	protected UserDirectoryService m_userDirectoryService = null;

	/**
	 * Dependency: UserDirectoryService.
	 * 
	 * @param service
	 *          The UserDirectoryService.
	 */
	public void setUserDirectoryService(UserDirectoryService service) {
		m_userDirectoryService = service;
	}

	/** Dependency: EventTrackingService */
	protected EventTrackingService m_eventTrackingService = null;

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *          The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service) {
		m_eventTrackingService = service;
	}

	/**
	 * 
	 */
	protected SecurityService securityService = null;
	/**
	 * 
	 */
	protected SiteService siteService = null;
	/**
	 * 
	 */
	protected ToolManager toolManager = null;

	/**
	 * Pull in any necessary services using factory pattern
	 */
	protected void getServices() {
		if (securityService == null)
			securityService = (SecurityService) ComponentManager
				.get("org.sakaiproject.authz.api.SecurityService");
		if (siteService == null)
			siteService = (SiteService) ComponentManager
				.get("org.sakaiproject.site.api.SiteService");
		if (toolManager == null)
			toolManager = (ToolManager) ComponentManager
				.get("org.sakaiproject.tool.api.ToolManager");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init() {
		try {
			M_log.info("init()");

		} catch (Exception t) {
			M_log.warn("init(): ", t);
		}

		getServices();

		// Check to see if all out properties are defined
		ArrayList<String> strings = foorm.checkI18NStrings(LTIService.TOOL_MODEL, rb);
		for (String str : strings) {
			M_log.warn(str + "=Missing LTIService Translation");
		}

		strings = foorm.checkI18NStrings(LTIService.CONTENT_MODEL, rb);
		for (String str : strings) {
			M_log.warn(str + "=Missing LTIService Translation");
		}

		strings = foorm.checkI18NStrings(LTIService.MAPPING_MODEL, rb);
		for (String str : strings) {
			M_log.warn(str + "=Missing LTIService Translation");
		}
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy() {
		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * LTIService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * 
	 */
	public String[] getMappingModel() {
		if (isAdmin())
			return MAPPING_MODEL;
		return null;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#getToolModel()
	 */
	public String[] getToolModel() {
		if (isAdmin())
			return TOOL_MODEL;
		if (isMaintain())
			return foorm.filterForm(null, TOOL_MODEL, null, ".*:role=admin.*");
		return null;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#getContentModel(java.lang.Long)
	 */
	public String[] getContentModel(Long tool_id) {
		if (!isMaintain()) return null;
		Map<String, Object> tool = getTool(tool_id);
		if (tool == null) return null;
		return getContentModel(tool);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#getContentModel(Map<String,Object>)
	 */
	public String[] getContentModel(Map<String, Object> tool) {
		String[] retval = foorm.filterForm(tool, CONTENT_MODEL);
		if (!isAdmin())
			retval = foorm.filterForm(null, retval, null, ".*:role=admin.*");
		return retval;
	}

	/**
	 * 
	 * @return
	 */
	protected String getContext() {
		String retval = toolManager.getCurrentPlacement().getContext();
		return retval;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#getContentLaunch(java.util.Map)
	 */
	public String getContentLaunch(Map<String, Object> content) {
		if ( content == null ) return null;
		int key = getInt(content.get(LTIService.LTI_ID));
		String siteId = (String) content.get(LTIService.LTI_SITE_ID);
		if (key < 0 || siteId == null)
			return null;
		return LAUNCH_PREFIX + siteId + "/content:" + key;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#formOutput(java.lang.Object,
	 *      java.lang.String)
	 */
	public String formOutput(Object row, String fieldInfo) {
		return foorm.formOutput(row, fieldInfo, rb);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#formOutput(java.lang.Object,
	 *      java.lang.String[])
	 */
	public String formOutput(Object row, String[] fieldInfo) {
		return foorm.formOutput(row, fieldInfo, rb);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#formInput(java.lang.Object,
	 *      java.lang.String)
	 */
	public String formInput(Object row, String fieldInfo) {
		return foorm.formInput(row, fieldInfo, rb);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#formInput(java.lang.Object,
	 *      java.lang.String[])
	 */
	public String formInput(Object row, String[] fieldInfo) {
		return foorm.formInput(row, fieldInfo, rb);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#isAdmin()
	 */
	public boolean isAdmin() {
		if (!ADMIN_SITE.equals(getContext()))
			return false;
		return isMaintain();
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#isMaintain()
	 */
	public boolean isMaintain() {
		return siteService.allowUpdateSite(getContext());
	}

	/**
	 * Simple API signatures for the update series of methods
	 */
	public Object updateTool(Long key, Map<String, Object> newProps) {
		return updateTool(key, (Object) newProps);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#updateTool(java.lang.Long,
	 *      java.util.Properties)
	 */
	public Object updateTool(Long key, Properties newProps) {
		return updateTool(key, (Object) newProps);
	}

	/**
	 * 
	 * @param key
	 * @param newProps
	 * @return
	 */
	public abstract Object updateTool(Long key, Object newProps);

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#updateMapping(java.lang.Long, java.util.Map)
	 */
	public Object updateMapping(Long key, Map<String, Object> newProps) {
		return updateMapping(key, (Object) newProps);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#updateMapping(java.lang.Long,
	 *      java.util.Properties)
	 */
	public Object updateMapping(Long key, Properties newProps) {
		return updateMapping(key, (Object) newProps);
	}

	/**
	 * 
	 * @param key
	 * @param newProps
	 * @return
	 */
	public abstract Object updateMapping(Long key, Object newProps);

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#updateContent(java.lang.Long, java.util.Map)
	 */
	public Object updateContent(Long key, Map<String, Object> newProps) {
		return updateContent(key, (Object) newProps);
	}

	/**
	 * 
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.lti.api.LTIService#updateContent(java.lang.Long,
	 *      java.util.Properties)
	 */
	public Object updateContent(Long key, Properties newProps) {
		return updateContent(key, (Object) newProps);
	}

	/**
	 * 
	 * @param key
	 * @param newProps
	 * @return
	 */
	public abstract Object updateContent(Long key, Object newProps);

	/**
	 * 
	 * @param o
	 * @return
	 */
	public static int getInt(Object o) {
		if (o instanceof String) {
			try {
				return (new Integer((String) o)).intValue();
			} catch (Exception e) {
				return -1;
			}
		}
		if (o instanceof Number)
			return ((Number) o).intValue();
		return -1;
	}

	// Adjust the content object based on the settings in the tool object
	/**
	 * 
	 */
	public void filterContent(Map<String, Object> content, Map<String, Object> tool) {
		if (content == null || tool == null)
			return;
		int heightOverride = getInt(tool.get(LTIService.LTI_ALLOWFRAMEHEIGHT));
		int toolHeight = getInt(tool.get(LTIService.LTI_FRAMEHEIGHT));
		int contentHeight = getInt(content.get(LTIService.LTI_FRAMEHEIGHT));
		int frameHeight = 1200;
		if (toolHeight > 0)
			frameHeight = toolHeight;
		if (heightOverride == 1 && contentHeight > 0)
			frameHeight = contentHeight;
		content.put(LTIService.LTI_FRAMEHEIGHT, new Integer(frameHeight));

		Integer newProp = null;
		newProp = getCorrectProperty("debug", content, tool);
		if (newProp != null)
			content.put("debug", newProp);

		newProp = getCorrectProperty(LTIService.LTI_NEWPAGE, content, tool);
		if (newProp != null)
			content.put(LTIService.LTI_NEWPAGE, newProp);
	}

	/**
	 * 
	 * @param propName
	 * @param content
	 * @param tool
	 * @return
	 */
	public static Integer getCorrectProperty(String propName, Map<String, Object> content,
			Map<String, Object> tool) {
		int toolProp = getInt(tool.get(propName));
		int contentProp = getInt(content.get(propName));
		if (toolProp == -1 || contentProp == -1)
			return null;

		int allowProp = getInt(tool.get("allow" + propName));
		int allowCode = -1;
		if (allowProp >= 0) {
			allowCode = allowProp;
		} else if (toolProp >= 0) {
			allowCode = toolProp;
		}

		// There is no control row assertion
		if (allowCode == -1)
			return null;

		// If the control property wants to override
		if (allowCode == 0 && toolProp != 0)
			return new Integer(0);
		if (allowCode == 1 && toolProp != 1)
			return new Integer(1);
		return null;
	}
	
	public Object insertToolContent(String id, String toolId, Properties reqProps)
	{
		Object retval = null;
		if ( ! isMaintain() ) {
			retval = rb.getString("error.maintain.edit");
		}
		if ( toolId == null ) {
			retval = rb.getString("error.id.not.found");
		}

		// Check to see if we have to fix the tool...
		String returnUrl = reqProps.getProperty("returnUrl");

		Long contentKey = null;
		if ( id == null ) 
		{
			retval = insertContent(reqProps);
		} else {
			contentKey = new Long(id);
			Long toolKey = new Long(toolId);
			Map<String,Object> tool = getTool(toolKey);
			if ( tool == null ) {
				retval = rb.getString("error.tool.not.found");
			}
			if ( returnUrl != null ) {
				if ( LTI_SECRET_INCOMPLETE.equals((String) tool.get(LTI_SECRET)) &&
						LTI_SECRET_INCOMPLETE.equals((String) tool.get(LTI_CONSUMERKEY)) ) {
					String reqSecret = reqProps.getProperty(LTIService.LTI_SECRET);
					String reqKey = reqProps.getProperty(LTIService.LTI_CONSUMERKEY);
					if ( reqSecret == null || reqKey == null || reqKey.trim().length() < 1 || reqSecret.trim().length() < 1 ) {
						retval = "0" + rb.getString("error.need.key.secret");
					}
					Properties toolProps = new Properties();
					toolProps.setProperty(LTI_SECRET, reqSecret);
					toolProps.setProperty(LTI_CONSUMERKEY, reqKey);
					updateTool(toolKey, toolProps);
				}
			}
			retval = updateContent(contentKey, reqProps);
		}
		return retval;
	}
	
	public Object insertToolSiteLink(String id, String button_text)
	{
		Object retval = null;
		
		if ( ! isMaintain() ) {
			retval = rb.getString("error.maintain.link");
			return retval;
		}
		
		if ( id == null ) {
			retval = new String("1" + rb.getString("error.id.not.found"));
			return retval;
		}
		
		Long key = new Long(id);
		Map<String,Object> content = getContent(key);
		if (  content == null ) {
			retval = new String("1" + rb.getString("error.content.not.found"));
			return retval;
		}
	
		String siteId = (String) content.get(LTI_SITE_ID);
		try
		{
			Site site = siteService.getSite(siteId);
			
			try
			{
				SitePage sitePage = site.addPage();
		
				ToolConfiguration tool = sitePage.addTool(WEB_PORTLET);
				String toolId = tool.getPageId();
				sitePage.setTitle(button_text);
				sitePage.setTitleCustom(true);
				
				
				siteService.save(site);
				
				tool.getPlacementConfig().setProperty("source",(String)content.get("launch_url"));
				tool.setTitle(button_text);
		
				tool.save();
		
				// Record the new placement in the content item
				Properties newProps = new Properties();
				newProps.setProperty(LTI_PLACEMENT, tool.getId());
				retval = updateContent(key, newProps);
			}
			catch (PermissionException ee)
			{
				retval = new String("0" + rb.getFormattedMessage("error.link.placement.update", new Object[]{id}));
				M_log.warn(this + " cannot add page and basic lti tool to site " + siteId);
			}
		}
		catch (IdUnusedException e)
		{
			// cannot find site
			retval = new String("0" + rb.getFormattedMessage("error.link.placement.update", new Object[]{id}));
			M_log.warn(this + " cannot find site " + siteId);
		}
				
		return retval;
	}

}
