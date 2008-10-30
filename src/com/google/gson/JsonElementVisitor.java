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

/**
 * Definition of a visitor for a JsonElement tree.
 * 
 * @author Inderjeet Singh
 */
interface JsonElementVisitor {
  void visitPrimitive(JsonPrimitive primitive);
  void visitNull();

  void startArray(JsonArray array);
  void visitArrayMember(JsonArray parent, JsonPrimitive member, boolean isFirst);
  void visitArrayMember(JsonArray parent, JsonArray member, boolean isFirst);
  void visitArrayMember(JsonArray parent, JsonObject member, boolean isFirst);
  void visitNullArrayMember(JsonArray parent, boolean isFirst);
  void endArray(JsonArray array);
  
  void startObject(JsonObject object);
  void visitObjectMember(JsonObject parent, String memberName, JsonPrimitive member, 
      boolean isFirst);
  void visitObjectMember(JsonObject parent, String memberName, JsonArray member, boolean isFirst);
  void visitObjectMember(JsonObject parent, String memberName, JsonObject member, boolean isFirst);
  void visitNullObjectMember(JsonObject parent, String memberName, boolean isFirst);
  void endObject(JsonObject object);
}