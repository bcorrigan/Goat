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

import java.lang.reflect.Type;

/**
 * A factory class used to simplify {@link ObjectNavigator} creation.
 * This object holds on to a reference of the {@link ExclusionStrategy}
 * that you'd like to use with the {@link ObjectNavigator}.
 *
 * @author Joel Leitch
 */
final class ObjectNavigatorFactory {
  private final ExclusionStrategy strategy;
  private final FieldNamingStrategy fieldNamingPolicy;
  private final MemoryRefStack<Object> stack;

  /**
   * Creates a factory object that will be able to create new
   * {@link ObjectNavigator}s with the provided {@code strategy}
   *
   * @param strategy the exclusion strategy to use with every instance that
   *        is created by this factory instance.
   * @param fieldNamingPolicy the naming policy that should be applied to field
   *        names
   */
  public ObjectNavigatorFactory(ExclusionStrategy strategy, FieldNamingStrategy fieldNamingPolicy) {
    Preconditions.checkNotNull(fieldNamingPolicy);
    this.strategy = (strategy == null ? new NullExclusionStrategy() : strategy);
    this.fieldNamingPolicy = fieldNamingPolicy;
    this.stack = new MemoryRefStack<Object>();
  }

  /**
   * Creates a new {@link ObjectNavigator} for this {@code srcObject}.
   *
   * @see #create(Object, Type)
   * @param srcObject object to navigate
   * @return a new instance of a {@link ObjectNavigator} ready to navigate the
   *         {@code srcObject}.
   */
  public ObjectNavigator create(Object srcObject) {
    return create(srcObject, srcObject.getClass());
  }

  /**
   * Creates a new {@link ObjectNavigator} for this {@code srcObject},
   * {@code type} pair.
   *
   * @param srcObject object to navigate
   * @param type the "actual" type of this {@code srcObject}.  NOTE: this can
   *        be a {@link java.lang.reflect.ParameterizedType} rather than a {@link Class}.
   * @return a new instance of a {@link ObjectNavigator} ready to navigate the
   *         {@code srcObject} while taking into consideration the
   *         {@code type}.
   */
  public ObjectNavigator create(Object srcObject, Type type) {
    return new ObjectNavigator(srcObject, type, strategy, stack);
  }

  FieldNamingStrategy getFieldNamingPolicy() {
    return fieldNamingPolicy;
  }
}
