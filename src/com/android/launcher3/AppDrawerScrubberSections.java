/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.launcher3;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class AppDrawerScrubberSections {
    private static final String TAG = AppDrawerScrubber.class.getSimpleName();
    private static final String ALPHA_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int MAX_NUMBER_CUSTOM_HEADERS = 8;
    private static final int MAX_HEADERS = ALPHA_LETTERS.length() + MAX_NUMBER_CUSTOM_HEADERS;
    public static final int INVALID_INDEX = -1;

    /** Header strings which have different strings in the scrubber **/
    private static final HashMap<String, String> sHeaderScrubberStringMap;
    static {
        sHeaderScrubberStringMap = new HashMap<String, String>();
        sHeaderScrubberStringMap.put(AppDrawerListAdapter.REMOTE_HEADER,
                AppDrawerListAdapter.REMOTE_SCRUBBER);
    }

    private AutoExpandTextView.HighlightedText mHighlightedText;
    private int mPreviousValidIndex;
    private int mNextValidIndex;
    private int mAdapterIndex;

    public AppDrawerScrubberSections(String text, boolean highlight, int idx) {
        if (sHeaderScrubberStringMap.get(text) != null) {
            text = sHeaderScrubberStringMap.get(text);
        }
        mHighlightedText = new AutoExpandTextView.HighlightedText(text, highlight);
        mAdapterIndex = idx;
        mPreviousValidIndex = mNextValidIndex = idx;
    }

    public boolean getHighlight() {
        return mHighlightedText.mHighlight;
    }

    public String getText() {
        return mHighlightedText.mText;
    }

    public int getPreviousIndex() {
        return mPreviousValidIndex;
    }

    public int getNextIndex() {
        return mNextValidIndex;
    }

    public int getAdapterIndex() {
        return mAdapterIndex;
    }

    private static int getFirstValidIndex(ArrayList<AppDrawerScrubberSections> sections) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).getHighlight()) {
                return i;
            }
        }

        return INVALID_INDEX;
    }

    private static void createIndices(ArrayList<AppDrawerScrubberSections> sections) {
        if (sections == null || sections.size() == 0) {
            return;
        }

        // walk forwards and fill out the previous valid index based on the previous highlight
        int currentIdx = getFirstValidIndex(sections);
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).getHighlight()) {
                currentIdx = i;
            }

            sections.get(i).mPreviousValidIndex = currentIdx;
        }

        // currentIdx should be now on the last valid index so walk back and fill the other way
        for (int i = sections.size() - 1; i >= 0; i--) {
            if (sections.get(i).getHighlight()) {
                currentIdx = i;
            }

            sections.get(i).mNextValidIndex = currentIdx;
        }
    }

    public static ArrayList<AutoExpandTextView.HighlightedText> getHighlightText(
            ArrayList<AppDrawerScrubberSections> sections) {
        if (sections == null) {
            return null;
        }

        ArrayList<AutoExpandTextView.HighlightedText> highlights = new ArrayList<>(sections.size());
        for (AppDrawerScrubberSections section : sections) {
            highlights.add(section.mHighlightedText);
        }

        return highlights;
    }

    private static void addAlphaLetters(ArrayList<AppDrawerScrubberSections> sections,
                                        HashMap<Integer, Integer> foundAlphaLetters) {
        for (int i = 0; i < ALPHA_LETTERS.length(); i++) {
            boolean highlighted = foundAlphaLetters.containsKey(i);
            int index = highlighted
                    ? foundAlphaLetters.get(i) : AppDrawerScrubberSections.INVALID_INDEX;

            sections.add(new AppDrawerScrubberSections(ALPHA_LETTERS.substring(i, i + 1),
                    highlighted, index));
        }
    }

    /**
     * Takes the headers and runs some checks to see if we can create a valid
     * appDrawerScrubberSection out of it.  This list will contain the original header list plus
     * fill out the remaining sections based on the ALPHA_LETTERS.  It will then determine which
     * ones to highlight as well as what letters to highlight when scrolling over the
     * grayed out sections
     * @param headers list of header Strings
     * @return the list of scrubber sections
     */
    public static ArrayList<AppDrawerScrubberSections> createSections(String[] headers) {
        // check if we have a valid header section
        if (!validHeaderList(headers)) {
            return null;
        }

        // this will track the mapping of ALPHA_LETTERS index to the headers index
        HashMap<Integer, Integer> foundAlphaLetters = new HashMap<>();
        ArrayList<AppDrawerScrubberSections> sections = new ArrayList<>(headers.length);
        boolean inAlphaLetterSection = false;

        for (int i = 0; i < headers.length; i++) {
            int alphaLetterIndex = TextUtils.isEmpty(headers[i])
                    ? -1 : ALPHA_LETTERS.indexOf(headers[i]);

            // if we found an ALPHA_LETTERS store that in foundAlphaLetters and continue
            if (alphaLetterIndex >= 0) {
                foundAlphaLetters.put(alphaLetterIndex, i);
                inAlphaLetterSection = true;
            } else {
                // if we are exiting the ALPHA_LETTERS section, add it here
                if (inAlphaLetterSection) {
                    addAlphaLetters(sections, foundAlphaLetters);
                    inAlphaLetterSection = false;
                }

                // add the custom header
                sections.add(new AppDrawerScrubberSections(headers[i], true, i));
            }
        }

        // if the last section are the alpha letters, then add it
        if (inAlphaLetterSection) {
            addAlphaLetters(sections, foundAlphaLetters);
        }

        // create the forward and backwards indices for scrolling over the grayed out sections
        AppDrawerScrubberSections.createIndices(sections);

        return sections;
    }

    /**
     * Walk through the headers and check for a few things:
     * 1) No more than MAX_NUMBER_CUSTOM_HEADERS headers exist in the headers list or no more
     * than MAX_HEADERS headers exist in the list
     * 2) the headers that fall in the ALPHA_LETTERS category are in the same order as ALPHA_LETTERS
     * 3) There are no headers that exceed length of 1
     * 4) The alpha letter section is together and not separated by other things
     */
    private static boolean validHeaderList(String[] headers) {
        int numCustomHeaders = 0;
        int previousAlphaIndex = -1;
        boolean foundAlphaHeaders = false;

        for (String s : headers) {
            if (TextUtils.isEmpty(s)) {
                numCustomHeaders++;
                continue;
            }

            if (s.length() > 1) {
                Log.w(TAG, "Found header " + s + " with length: " + s.length());
                return false;
            }

            int alphaIndex = ALPHA_LETTERS.indexOf(s);
            if (alphaIndex >= 0) {
                if (previousAlphaIndex != -1) {
                    // if the previous alpha index is >= alphaIndex then it is in the wrong order
                    if (previousAlphaIndex >= alphaIndex) {
                        Log.w(TAG, "Found letter index " + previousAlphaIndex
                                + " which is greater than " + alphaIndex);
                        return false;
                    }
                }

                // if we've found headers previously and the index is -1 that means the alpha
                // letters are separated out into two sections so return false
                if (foundAlphaHeaders && previousAlphaIndex == -1) {
                    Log.w(TAG, "Found alpha letters twice");
                    return false;
                }

                previousAlphaIndex = alphaIndex;
                foundAlphaHeaders = true;
            } else {
                numCustomHeaders++;
                previousAlphaIndex = -1;
            }
        }

        final int listSize = foundAlphaHeaders
                ? numCustomHeaders + ALPHA_LETTERS.length()
                : numCustomHeaders;

        // if one of these conditions are satisfied, then return true
        if (numCustomHeaders <= MAX_NUMBER_CUSTOM_HEADERS || listSize <= MAX_HEADERS) {
            return true;
        }

        if (numCustomHeaders > MAX_NUMBER_CUSTOM_HEADERS) {
            Log.w(TAG, "Found " + numCustomHeaders + "# custom headers when " +
                    MAX_NUMBER_CUSTOM_HEADERS + " is allowed!");
        } else if (listSize > MAX_HEADERS) {
            Log.w(TAG, "Found " + listSize + " headers when " +
                    MAX_HEADERS + " is allowed!");
        }

        return false;
    }
}