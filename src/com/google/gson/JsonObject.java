/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A class representing an object type in Json. An object consists of name-value pairs where names 
 * are strings, and values are any other type of {@link JsonElement}. This allows for a creating a 
 * tree of JsonElements. The member elements of this object are maintained in order they were added. 
 * 
 * @author Inderjeet Singh
 */
public final class JsonObject extends JsonElement {
  // We are using a linked hash map because it is important to preserve
  // the order in which elements are inserted. This is needed to ensure
  // that the fields of an object are inserted in the order they were 
  // defined in the class. 
  private final Map<String, JsonElement> members;

  /**
   * Creates an empty JsonObject.
   */
  public JsonObject() {
    members = new LinkedHashMap<String, JsonElement>();
  }
  
  /**
   * Adds a member, which is a name-value pair, to self. The name must be a String, but the value
   * can be an arbitrary JsonElement, thereby allowing you to build a full tree of JsonElements
   * rooted at this node. 
   *   
   * @param property name of the member.
   * @param value the member object.
   */
  public void add(String property, JsonElement value) {
    members.put(property, value);
  }
  
  /**
   * Convenience method to add a primitive member. The specified value is converted to a 
   * JsonPrimitive of String. 
   *  
   * @param property name of the member.
   * @param value the string value associated with the member.
   */
  public void addProperty(String property, String value) {
    members.put(property, new JsonPrimitive(value));
  }
  
  /**
   * Convenience method to add a primitive member. The specified value is converted to a 
   * JsonPrimitive of Number. 
   *  
   * @param property name of the member.
   * @param value the number value associated with the member.
   */
  public void addProperty(String property, Number value) {
    members.put(property, new JsonPrimitive(value));
  }

  /**
   * Returns a set of members of this object. The set is ordered, and the order is in which the 
   * elements were added. 
   *  
   * @return a set of members of this object. 
   */
  public Set<Entry<String, JsonElement>> entrySet() {
    return members.entrySet();
  }
  
  /**
   * Convenience method to check if a member with the specified name is present in this object. 
   * 
   * @param memberName name of the member that is being checked for presence.
   * @return true if there is a member with the specified name, false otherwise. 
   */
  public boolean has(String memberName) {
    return members.containsKey(memberName);
  }
  
  /**
   * Returns the member with the specified name. 
   * 
   * @param memberName name of the member that is being requested.
   * @return the member matching the name. Null if no such member exists. 
   */
  public JsonElement get(String memberName) {
    return members.get(memberName);
  }
  
  /**
   * Convenience method to get the specified member as a JsonPrimitive element. 
   * 
   * @param memberName name of the member being requested. 
   * @return the JsonPrimitive corresponding to the specified member. 
   */
  public JsonPrimitive getAsJsonPrimitive(String memberName) {
    return (JsonPrimitive) members.get(memberName);
  }
  
  /**
   * Convenience method to get the specified member as a JsonArray.
   * 
   * @param memberName name of the member being requested. 
   * @return the JsonArray corresponding to the specified member.
   */
  public JsonArray getAsJsonArray(String memberName) {
    return (JsonArray) members.get(memberName);
  }

  /**
   * Convenience method to get the specified member as a JsonObject.
   * 
   * @param memberName name of the member being requested. 
   * @return the JsonObject corresponding to the specified member.
   */
  public JsonObject getAsJsonObject(String memberName) {
    return (JsonObject) members.get(memberName);
  }

  @Override
  protected void toString(StringBuilder sb) {
    sb.append('{');
    boolean first = true;
    for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      sb.append('\"');
      sb.append(entry.getKey());
      sb.append("\":");
      entry.getValue().toString(sb);
    }
    sb.append('}');
  }
}
