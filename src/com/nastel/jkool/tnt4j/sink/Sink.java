/*
 * Copyright 2014 Nastel Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nastel.jkool.tnt4j.sink;

import java.io.IOException;

/**
 * <p>
 * This interface defines a message destination end point. Message destination is
 * an entity that can be opened/closed as well as written to.
 * </p>
 * 
 * 
 * @version $Revision: 3 $
 * 
 */
public interface Sink extends Handle {
	/**
	 * This method returns a connection handle associated with
	 * the message destination. 
	 * 
	 * @return underlying sink handle.
	 */
	public Object getSinkHandle();
		

	/**
	 * This method allows writing to the underlying message destination
	 * 
	 * @param msg message to be written to the sink
	 * @param args arguments associated with the message
	 * @throws IOException
	 */
	public void write(Object msg, Object...args) throws IOException;
}
