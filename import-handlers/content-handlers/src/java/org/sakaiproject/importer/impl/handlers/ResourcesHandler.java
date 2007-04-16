/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.importer.impl.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.InconsistentException;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.importer.api.HandlesImportable;
import org.sakaiproject.importer.api.Importable;
import org.sakaiproject.importer.impl.importables.FileResource;
import org.sakaiproject.importer.impl.importables.Folder;
import org.sakaiproject.importer.impl.importables.WebLink;
import org.sakaiproject.importer.impl.importables.HtmlDocument;
import org.sakaiproject.importer.impl.importables.TextDocument;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.util.Validator;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class ResourcesHandler implements HandlesImportable {
	private static final String COPYRIGHT = "(c) 2007";
	
	private Log m_log = LogFactory.getLog(org.sakaiproject.importer.impl.handlers.ResourcesHandler.class);

	public boolean canHandleType(String typeName) {
		return (("sakai-file-resource".equals(typeName) || ("sakai-folder".equals(typeName)) || 
				("sakai-text-document".equals(typeName)) || ("sakai-html-document".equals(typeName)) || 
				("sakai-web-link".equals(typeName)) || ("sakai-learning-module".equals(typeName))));
	}

	public void handle(Importable thing, String siteId) {
		if(canHandleType(thing.getTypeName())){
			String currentUser = SessionManager.getCurrentSessionUserId();
			SessionManager.getCurrentSession().setUserId("admin");
			String id = null;
			String contentType = null;
			byte[] contents = null;
			int notifyOption = NotificationService.NOTI_NONE;
			String title = null;
			String description = null;
			Map resourceProps = new HashMap();
			if ("sakai-file-resource".equals(thing.getTypeName())) {
				//title = ((FileResource)thing).getTitle();
				description = ((FileResource)thing).getDescription();
				String fileName = ((FileResource)thing).getFileName();
				id = ContentHostingService.getSiteCollection(siteId) + ((FileResource)thing).getDestinationResourcePath();
				contentType = new MimetypesFileTypeMap().getContentType(fileName);
				contents = ((FileResource)thing).getFileBytes();
//				if((title == null) || (title.equals(""))) {
//					title = fileName;
//				}
				title = fileName;
				resourceProps.put(ResourceProperties.PROP_DESCRIPTION, description);

				
				if (title.endsWith(".zip")) {
					
					//create a folder with the name of the zip, minus the .zip
					String container = title.substring(0, title.length() - 4);
					resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, container);

					//get the full path to the current folder
					String path = id.substring(0, id.length() - title.length());

					addContentCollection(path + container, resourceProps);
		  			addAllResources(contents, path + container, notifyOption);
						
				}
				else {
					if(m_log.isDebugEnabled()) {
						m_log.debug("import ResourcesHandler about to add file entitled '" + title + "'");
					}
					resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, title);
					addContentResource(id, contentType, contents, resourceProps, notifyOption);
				}				
	  			
			} else if ("sakai-web-link".equals(thing.getTypeName())) {
				title = ((WebLink)thing).getTitle();
				description = ((WebLink)thing).getDescription();
				id = ContentHostingService.getSiteCollection(siteId) + thing.getContextPath();
				contentType = ResourceProperties.TYPE_URL;
				String absoluteUrl = "";
				if (((WebLink)thing).isAbsolute()) {
					absoluteUrl = ((WebLink)thing).getUrl();
				} else {
					absoluteUrl = ServerConfigurationService.getServerUrl() + "/access/content" + 
						ContentHostingService.getSiteCollection(siteId) + ((WebLink)thing).getUrl();
				}
				contents = absoluteUrl.getBytes();
				if((title == null) || (title.equals(""))) {
					title = ((WebLink)thing).getUrl();
				}
				resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, title);
				resourceProps.put(ResourceProperties.PROP_DESCRIPTION, description);
				resourceProps.put(ResourceProperties.PROP_HAS_CUSTOM_SORT, Boolean.TRUE.toString());
				resourceProps.put(ResourceProperties.PROP_CONTENT_PRIORITY, Integer.toString(((WebLink)thing).getSequenceNum()));
				if(m_log.isDebugEnabled()){ 
					m_log.debug("import ResourcesHandler about to add web link entitled '" + title + "'");
				}
				addContentResource(id, contentType, contents, resourceProps, notifyOption);
			} else if ("sakai-html-document".equals(thing.getTypeName())) {
				title = ((HtmlDocument)thing).getTitle();
				contents = ((HtmlDocument)thing).getContent().getBytes();
				id = ContentHostingService.getSiteCollection(siteId) + thing.getContextPath();
				contentType = "text/html";
				resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, title);
				if(m_log.isDebugEnabled()){ 
					m_log.debug("import ResourcesHandler about to add html document entitled '" + title + "'");
				}
				addContentResource(id, contentType, contents, resourceProps, notifyOption);
			} else if ("sakai-text-document".equals(thing.getTypeName())) {
				title = ((TextDocument)thing).getTitle();
				contents = ((TextDocument)thing).getContent().getBytes();
				id = ContentHostingService.getSiteCollection(siteId) + thing.getContextPath();
				contentType = "text/plain";
				resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, title);
				if(m_log.isDebugEnabled()){ 
					m_log.debug("import ResourcesHandler about to add text document entitled '" + title + "'");
				}
				addContentResource(id, contentType, contents, resourceProps, notifyOption);
			} 
		    else if ("sakai-folder".equals(thing.getTypeName())) {
		    	title = ((Folder)thing).getTitle();
				description = ((Folder)thing).getDescription();
				resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, title);
	  			resourceProps.put(ResourceProperties.PROP_DESCRIPTION, description);
	  			resourceProps.put(ResourceProperties.PROP_COPYRIGHT, COPYRIGHT);
	  			/*
	  			 * Added title to the end of the path. Otherwise, we're setting the props on the 
	  			 * containing folder rather than the folder itself.
	  			 */
	  			String path = ContentHostingService.getSiteCollection(siteId) + ((Folder)thing).getPath();
	    		addContentCollection(path,resourceProps);

			}
			SessionManager.getCurrentSession().setUserId(currentUser);
		}

	}
	
	protected void addAllResources(byte[] archive, String path, int notifyOption) {
		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(archive));
		ZipEntry entry;
		String contentType;
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		try {
			entry = (ZipEntry) zipStream.getNextEntry();
			while (entry != null) {
				Map resourceProps = new HashMap();
				contentType = new MimetypesFileTypeMap().getContentType(entry.getName());
				String title = entry.getName();
				if (title.lastIndexOf("/") > 0) {
					title = title.substring(title.lastIndexOf("/") + 1);
				}
				resourceProps.put(ResourceProperties.PROP_DISPLAY_NAME, title);
				resourceProps.put(ResourceProperties.PROP_COPYRIGHT, COPYRIGHT);
				if(m_log.isDebugEnabled()) {
					m_log.debug("import ResourcesHandler about to add file entitled '" + title + "'");
				}

				int size = (int) entry.getSize();
				// -1 means unknown size.
				if (size != -1) {

					byte[] contents = new byte[(int) size];
					int returnBytes = 0;
					int chunk = 0;
					while (((int)size - returnBytes) > 0) {
						chunk = zipStream.read(contents,returnBytes,(int)size - returnBytes);
						if (chunk == -1) {
							break;
						}
						returnBytes += chunk;
					}

					if (entry.isDirectory()) {

						addContentCollection(path + entry.getName(), resourceProps);
						addAllResources(contents, path + entry.getName(), notifyOption);
					}
					else {
						addContentResource(path + entry.getName(), contentType, contents, resourceProps, notifyOption);
					}
					entry = (ZipEntry) zipStream.getNextEntry();
				}
				else {
					if(m_log.isWarnEnabled()) {
						m_log.warn("Zip file is of unknown size... giving up");
					}					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	protected void addContentResource(String id, String contentType, byte[] contents, Map properties, int notifyOption) {
		try {
			id = makeIdClean(id);
			ResourcePropertiesEdit resourceProps = ContentHostingService.newResourceProperties();
			Set keys = properties.keySet();
			for (Iterator i = keys.iterator();i.hasNext();) {
				String key = (String)i.next();
				String value = (String)properties.get(key);
				resourceProps.addProperty(key, value);
			}
			String enclosingDirectory = id.substring(0, id.lastIndexOf('/', id.length() - 2) + 1);
			if(existsDirectory(enclosingDirectory)) {
				ContentHostingService.addProperty(enclosingDirectory, ResourceProperties.PROP_HAS_CUSTOM_SORT, Boolean.TRUE.toString());
			}
			ContentHostingService.addResource(id, contentType, contents, resourceProps, notifyOption);
		} catch (PermissionException e) {
			m_log.error("ResourcesHandler.addContentResource: " + e.toString());
		} catch (IdUsedException e) {
			m_log.warn("ResourcesHandler.addContentResource IdUsedException: " + e.toString());
		} catch (IdInvalidException e) {
//			TODO Auto-generated catch block
            e.printStackTrace();
		} catch (InconsistentException e) {
//			TODO Auto-generated catch block
            e.printStackTrace();
		} catch (OverQuotaException e) {
//			TODO Auto-generated catch block
            e.printStackTrace();
		} catch (ServerOverloadException e) {
//			TODO Auto-generated catch block
            e.printStackTrace();
		} catch (IdUnusedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	protected boolean existsDirectory(String path) {
		try {
			ContentHostingService.getCollection(path);
		} catch (IdUnusedException e) {
			return false;
		} catch (TypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PermissionException e) {
			m_log.error("ResourcesHandler.existsDirectory: " + e.toString());
		}
		return true;
	}

	protected void addContentCollection(String path, Map properties) {
			path = makeIdClean(path);
			ResourcePropertiesEdit resourceProps = ContentHostingService.newResourceProperties();
			Set keys = properties.keySet();
			for (Iterator i = keys.iterator();i.hasNext();) {
				String key = (String)i.next();
				String value = (String)properties.get(key);
				resourceProps.addProperty(key, value);
			}
//			ContentCollectionEdit coll = null;
			try {
//				String enclosingDirectory = path.substring(0, path.lastIndexOf('/', path.length() - 2) + 1);
//				if(!existsDirectory(enclosingDirectory)) {
//					ResourcePropertiesEdit enclosingProps = ContentHostingService.newResourceProperties();
//					enclosingProps.addProperty(ResourceProperties.PROP_DISPLAY_NAME, );
//					ContentHostingService.addCollection(enclosingDirectory, enclosingProps);
//
//				}

				ContentHostingService.addCollection(path, resourceProps);
				//coll = ContentHostingService.addCollection(path);
                //ContentHostingService.commitCollection(coll);
				
			} catch (IdUsedException e) {
                // if this thing already exists (which it probably does), 
                // we'll do an update on the properties rather than creating the folder
//				try {
//					ContentHostingService.addProperty
//						(path, ResourceProperties.PROP_DISPLAY_NAME, (String)properties.get(ResourceProperties.PROP_DISPLAY_NAME));
//					ContentHostingService.addProperty
//						(path, ResourceProperties.PROP_COPYRIGHT, (String)properties.get(ResourceProperties.PROP_COPYRIGHT));
//					ContentHostingService.addProperty
//						(path, ResourceProperties.PROP_DESCRIPTION, (String)properties.get(ResourceProperties.PROP_DESCRIPTION));
//				} catch (PermissionException e1) {
//					m_log.error("ResourcesHandler.addContentCollection: " + e.toString());
//				} catch (IdUnusedException e1) {
//		           // TODO Auto-generated catch block
//		           e1.printStackTrace();
//				} catch (TypeException e1) {
//		           // TODO Auto-generated catch block
//		           e1.printStackTrace();
//				} catch (InUseException e1) {
//		           // TODO Auto-generated catch block
//		           e1.printStackTrace();
//				} catch (ServerOverloadException e1) {
//		           // TODO Auto-generated catch block
//		           e1.printStackTrace();
//				}

			} catch (IdInvalidException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PermissionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InconsistentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	}
	
	private String makeIdClean (String path) {
		String [] parts = path.split("/");
		String rv = "";
		
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() > 0) {
				rv += "/" + Validator.escapeResourceName(parts[i]);
			}
		}
		
		return rv;
	}

}
