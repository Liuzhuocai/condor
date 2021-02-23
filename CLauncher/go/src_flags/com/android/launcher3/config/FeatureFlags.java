/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.config;

/**
 * Defines a set of flags used to control various launcher behaviors
 */
public final class FeatureFlags extends BaseFlags {

    private FeatureFlags() {}

    // Features to control Launcher3Go behavior
    public static  boolean GO_DISABLE_WIDGETS = true;
    public static final boolean LAUNCHER3_SPRING_ICONS = false;
    //liuzuo :add for GMS to adjust widget visibility:start
    public static final boolean GO_PROJECT = true;
    //liuzuo :add for GMS to adjust widget visibility:end

    //Bruce:add to unread message:start^M
    public final static boolean UNREAD_MESSAGE = true;
    //Bruce:add to unread message:end
    public final static boolean MINUS_ONE = false;
}