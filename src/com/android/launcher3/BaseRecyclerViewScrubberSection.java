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
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class BaseRecyclerViewScrubberSection {
    private static final String TAG = "BRVScrubberSections";
    private static final String ALPHA_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int MAX_NUMBER_CUSTOM_SECTIONS = 8;
    private static final int MAX_SECTIONS = ALPHA_LETTERS.length() + MAX_NUMBER_CUSTOM_SECTIONS;
    public static final int INVALID_INDEX = -1;

    private AutoExpandTextView.HighlightedText mHighlightedText;
    private int mPreviousValidIndex;
    private int mNextValidIndex;
    private int mAdapterIndex;

    public BaseRecyclerViewScrubberSection(String text, boolean highlight, int idx) {
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

    private static int
        getFirstValidIndex(RtlIndexArrayList<BaseRecyclerViewScrubberSection> sections,
            boolean isRtl) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i, isRtl).getHighlight()) {
                return i;
            }
        }

        return INVALID_INDEX;
    }

    private static void createIndices(RtlIndexArrayList<BaseRecyclerViewScrubberSection> sections,
            boolean isRtl) {
        if (sections == null || sections.size() == 0) {
            return;
        }

        // walk forwards and fill out the previous valid index based on the previous highlight
        int currentIdx = getFirstValidIndex(sections, isRtl);
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i, isRtl).getHighlight()) {
                currentIdx = i;
            }

            sections.get(i, isRtl).mPreviousValidIndex = currentIdx;
        }

        // currentIdx should be now on the last valid index so walk back and fill the other way
        for (int i = sections.size() - 1; i >= 0; i--) {
            if (sections.get(i, isRtl).getHighlight()) {
                currentIdx = i;
            }

            sections.get(i, isRtl).mNextValidIndex = currentIdx;
        }
    }

    public static ArrayList<AutoExpandTextView.HighlightedText> getHighlightText(
            RtlIndexArrayList<BaseRecyclerViewScrubberSection> sections) {
        if (sections == null) {
            return null;
        }

        ArrayList<AutoExpandTextView.HighlightedText> highlights = new ArrayList<>(sections.size());
        for (BaseRecyclerViewScrubberSection section : sections) {
            highlights.add(section.mHighlightedText);
        }

        return highlights;
    }

    private static void addAlphaLetters(RtlIndexArrayList<BaseRecyclerViewScrubberSection> sections,
                                        HashMap<Integer, Integer> foundAlphaLetters) {
        for (int i = 0; i < ALPHA_LETTERS.length(); i++) {
            boolean highlighted = foundAlphaLetters.containsKey(i);
            int index = highlighted
                    ? foundAlphaLetters.get(i) : BaseRecyclerViewScrubberSection.INVALID_INDEX;

            sections.add(new BaseRecyclerViewScrubberSection(ALPHA_LETTERS.substring(i, i + 1),
                    highlighted, index));
        }
    }

    /**
     * Takes the sections and runs some checks to see if we can create a valid
     * appDrawerScrubberSection out of it.  This list will contain the original header list plus
     * fill out the remaining sections based on the ALPHA_LETTERS.  It will then determine which
     * ones to highlight as well as what letters to highlight when scrolling over the
     * grayed out sections
     * @param sectionNames list of sectionName Strings
     * @return the list of scrubber sections
     */
    public static RtlIndexArrayList<BaseRecyclerViewScrubberSection>
        createSections(String[] sectionNames, boolean isRtl) {
        // check if we have a valid header section
        if (!validSectionNameList(sectionNames)) {
            return null;
        }

        // this will track the mapping of ALPHA_LETTERS index to the headers index
        HashMap<Integer, Integer> foundAlphaLetters = new HashMap<>();
        RtlIndexArrayList<BaseRecyclerViewScrubberSection> sections =
                new RtlIndexArrayList<>(sectionNames.length);
        boolean inAlphaLetterSection = false;

        for (int i = 0; i < sectionNames.length; i++) {
            int alphaLetterIndex = TextUtils.isEmpty(sectionNames[i])
                    ? -1 : ALPHA_LETTERS.indexOf(sectionNames[i]);

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
                sections.add(new BaseRecyclerViewScrubberSection(sectionNames[i], true, i));
            }
        }

        // if the last section are the alpha letters, then add it
        if (inAlphaLetterSection) {
            addAlphaLetters(sections, foundAlphaLetters);
        }

        // create the forward and backwards indices for scrolling over the grayed out sections
        BaseRecyclerViewScrubberSection.createIndices(sections, isRtl);

        return sections;
    }

    /**
     * Walk through the sectionNames and check for a few things:
     * 1) No more than MAX_NUMBER_CUSTOM_SECTIONS sectionNames exist in the sectionNames list or no more
     * than MAX_SECTIONS sectionNames exist in the list
     * 2) the headers that fall in the ALPHA_LETTERS category are in the same order as ALPHA_LETTERS
     * 3) There are no sectionNames that exceed length of 1
     * 4) The alpha letter sectionName is together and not separated by other things
     */
    private static boolean validSectionNameList(String[] sectionNames) {
        int numCustomSections = 0;
        int previousAlphaIndex = -1;
        boolean foundAlphaSections = false;

        for (String s : sectionNames) {
            if (TextUtils.isEmpty(s)) {
                numCustomSections++;
                continue;
            }

            if (s.length() > 1) {
                Log.w(TAG, "Found section " + s + " with length: " + s.length());
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
                if (foundAlphaSections && previousAlphaIndex == -1) {
                    Log.w(TAG, "Found alpha letters twice");
                    return false;
                }

                previousAlphaIndex = alphaIndex;
                foundAlphaSections = true;
            } else {
                numCustomSections++;
                previousAlphaIndex = -1;
            }
        }

        final int listSize = foundAlphaSections
                ? numCustomSections + ALPHA_LETTERS.length()
                : numCustomSections;

        // if one of these conditions are satisfied, then return true
        if (numCustomSections <= MAX_NUMBER_CUSTOM_SECTIONS || listSize <= MAX_SECTIONS) {
            return true;
        }

        if (numCustomSections > MAX_NUMBER_CUSTOM_SECTIONS) {
            Log.w(TAG, "Found " + numCustomSections + "# custom sections when " +
                    MAX_NUMBER_CUSTOM_SECTIONS + " is allowed!");
        } else if (listSize > MAX_SECTIONS) {
            Log.w(TAG, "Found " + listSize + " sections when " +
                    MAX_SECTIONS + " is allowed!");
        }

        return false;
    }

    public static class RtlIndexArrayList<T> extends ArrayList<T>  {

        public RtlIndexArrayList(int capacity) {
            super(capacity);
        }

        public T get(int index, boolean isRtl) {
            if (isRtl) {
                index = size() - 1 - index;
            }
            return super.get(index);
        }
    }
}