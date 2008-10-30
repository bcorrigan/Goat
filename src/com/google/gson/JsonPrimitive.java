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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A class representing a Json primitive value. A primitive value
 * is either a String, a Java primitive, or a Java primitive
 * wrapper type.
 *
 * @author Inderjeet Singh
 */
public final class JsonPrimitive extends JsonElement {

  private Object value;

  /**
   * Create a primitive containing a boolean value.
   *
   * @param bool the value to create the primitive with.
   */
  public JsonPrimitive(Boolean bool) {
    this.value = bool;
  }

  /**
   * Create a primitive containing a {@link Number}.
   *
   * @param number the value to create the primitive with.
   */
  public JsonPrimitive(Number number) {
    this.value = number;
  }

  /**
   * Create a primitive containing a String value.
   *
   * @param string the value to create the primitive with.
   */
  public JsonPrimitive(String string) {
    this.value = string;
  }

  /**
   * Create a primitive containing a character. The character is turned into a one character String
   * since Json only supports String.
   *
   * @param c the value to create the primitive with.
   */
  public JsonPrimitive(Character c) {
    this.value = String.valueOf(c);
  }

  /**
   * Create a primitive containing a character. The character is turned into a one character String
   * since Json only supports String.
   *
   * @param c the value to create the primitive with.
   */
  public JsonPrimitive(char c) {
    this.value = String.valueOf(c);
  }

  /**
   * Create a primitive using the specified Object. It must be an instance of {@link Number}, a
   * Java primitive type, or a String.
   *
   * @param primitive the value to create the primitive with.
   */
  JsonPrimitive(Object primitive) {
    setValue(primitive);
  }

  void setValue(Object primitive) {
    if (primitive instanceof Character) {
      // convert characters to strings since in JSON, characters are represented as a single
      // character string
      char c = ((Character)primitive).charValue();
      this.value = String.valueOf(c);
    } else {
      Preconditions.checkArgument(primitive instanceof Number
          || ObjectNavigator.isPrimitiveOrString(primitive));
      this.value = primitive;
    }
  }

  /**
   * Check whether this primitive contains a boolean value.
   *
   * @return true if this primitive contains a boolean value, false otherwise.
   */
  public boolean isBoolean() {
    return value instanceof Boolean;
  }

  /**
   * convenience method to get this element as a {@link Boolean}.
   *
   * @return get this element as a {@link Boolean}.
   * @throws ClassCastException if the value contained is not a valid boolean value.
   */
  @Override
  Boolean getAsBooleanWrapper() {
    return (Boolean) value;
  }

  /**
   * convenience method to get this element as a boolean value.
   *
   * @return get this element as a primitive boolean value.
   * @throws ClassCastException if the value contained is not a valid boolean value.
   */
  @Override
  public boolean getAsBoolean() {
    return ((Boolean) value).booleanValue();
  }

  /**
   * Check whether this primitive contains a Number.
   *
   * @return true if this primitive contains a Number, false otherwise.
   */
  public boolean isNumber() {
    return value instanceof Number;
  }

  /**
   * convenience method to get this element as a Number.
   *
   * @return get this element as a Number.
   * @throws ClassCastException if the value contained is not a valid Number.
   */
  @Override
  public Number getAsNumber() {
    return (Number) value;
  }

  /**
   * Check whether this primitive contains a String value.
   *
   * @return true if this primitive contains a String value, false otherwise.
   */
  public boolean isString() {
    return value instanceof String;
  }

  /**
   * convenience method to get this element as a String.
   *
   * @return get this element as a String.
   * @throws ClassCastException if the value contained is not a valid String.
   */
  @Override
  public String getAsString() {
    return (String) value;
  }

  /**
   * convenience method to get this element as a primitive double.
   *
   * @return get this element as a primitive double.
   * @throws ClassCastException if the value contained is not a valid double.
   */
  @Override
  public double getAsDouble() {
    return ((Number) value).doubleValue();
  }

  /**
   * convenience method to get this element as a {@link BigDecimal}.
   *
   * @return get this element as a {@link BigDecimal}.
   * @throws NumberFormatException if the value contained is not a valid {@link BigDecimal}.
   */
  @Override
  public BigDecimal getAsBigDecimal() {
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    } else {
      return new BigDecimal(value.toString());
    }
  }

  /**
   * convenience method to get this element as a {@link BigInteger}.
   *
   * @return get this element as a {@link BigInteger}.
   * @throws NumberFormatException if the value contained is not a valid {@link BigInteger}.
   */
  @Override
  public BigInteger getAsBigInteger() {
    if (value instanceof BigInteger) {
      return (BigInteger) value;
    } else {
      return new BigInteger(value.toString());
    }
  }

  /**
   * convenience method to get this element as a float.
   *
   * @return get this element as a float.
   * @throws ClassCastException if the value contained is not a valid float.
   */
  @Override
  public float getAsFloat() {
    return ((Number) value).floatValue();
  }

  /**
   * convenience method to get this element as a primitive long.
   *
   * @return get this element as a primitive long.
   * @throws ClassCastException if the value contained is not a valid long.
   */
  @Override
  public long getAsLong() {
    return ((Number) value).longValue();
  }

  /**
   * convenience method to get this element as a primitive short.
   *
   * @return get this element as a primitive short.
   * @throws ClassCastException if the value contained is not a valid short value.
   */
 @Override
  public short getAsShort() {
    return ((Number) value).shortValue();
  }

 /**
  * convenience method to get this element as a primitive integer.
  *
  * @return get this element as a primitive integer.
  * @throws ClassCastException if the value contained is not a valid integer.
  */
  @Override
  public int getAsInt() {
    return ((Number) value).intValue();
  }

  /**
   * convenience method to get this element as an Object.
   *
   * @return get this element as an Object that can be converted to a suitable value.
   */
  @Override
  Object getAsObject() {
    return value;
  }

  @Override
  protected void toString(StringBuilder sb) {
    if (value != null) {
      if (value instanceof String) {
        sb.append('"');
        sb.append(value);
        sb.append('"');

      } else {
        sb.append(value);
      }
    }
  }
}
