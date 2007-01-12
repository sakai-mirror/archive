/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

package org.sakaiproject.importer.api;

/**
 * Defines the minimum attributes for an object that can be pushed into Sakai by one or more import handlers.
 *
 * @author <a href="mailto:zach.thomas@txstate.edu">Zach A. Thomas</a>
 */
public interface Importable {
	/**
	 * @return a globally unique identifier for this object
	 */
	String getGuid();
	/**
	 * @return a String identifying the semantic type of this object, e.g. "sakai-qti-assessment"
	 */
	String getTypeName();
	/**
	 * @return a label that identifies a logical grouping that this object occupied in its 
	 *         previous home, e.g. "Course Documents" from a Blackboard system
	 */
	String getLegacyGroup();
	/**
	 * @return a path that can be used to place this object in the appropriate place in Sakai, 
	 *         e.g. "/Chapter 2/review"
	 */
	String getContextPath();
	/**
	 * @return another Importable that can be considered the logical container of this object, 
	 *         e.g. a question pool can be the parent of a question
	 */
	Importable getParent();
	/**
	 * @param another Importable that can be considered the logical container of this object, 
	 *        e.g. a question pool can be the parent of a question
	 */
	void setParent(Importable parent);
	/**
	 * @param a label that identifies a logical grouping that this object occupied in its 
	 *        previous home, e.g. "Course Documents" from a Blackboard system
	 */
	void setLegacyGroup(String legacyGroup);
	/**
	 * @param a path that can be used to place this object in the appropriate place in Sakai, 
	 *        e.g. "/Chapter 2/review"
	 */
	void setContextPath(String path);
}
