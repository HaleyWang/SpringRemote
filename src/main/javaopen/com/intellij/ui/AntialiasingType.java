/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.util.ui.UIUtil;
import sun.swing.SwingUtilities2;

import java.awt.*;

public enum AntialiasingType {
  SUBPIXEL("Subpixel", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB, true),
  GREYSCALE("Greyscale", RenderingHints.VALUE_TEXT_ANTIALIAS_ON, true),
  OFF("No antialiasing", RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, false);

  public static Object getAAHintForSwingComponent() {
    return GREYSCALE.getTextInfo();
  }



  private final String myName;
  private final Object myHint;
  private final boolean isEnabled;

  AntialiasingType(String name, Object hint, boolean enabled) {
    myName = name;
    myHint = hint;
    isEnabled = enabled;
  }

  public SwingUtilities2.AATextInfo getTextInfo() {
    return !isEnabled ? null : new SwingUtilities2.AATextInfo(myHint, UIUtil.getLcdContrastValue());
  }

  @Override
  public String toString() {
    return myName;
  }
 }
