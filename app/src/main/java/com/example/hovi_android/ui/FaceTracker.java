package com.example.hovi_android.ui;

/*
 * Copyright (C) The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.util.Log;

import com.example.hovi_android.EyeActivity;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Tracks the eye positions and state over time, managing an underlying graphic which renders googly
 * eyes over the source video.<p>
 *
 * To improve eye tracking performance, it also helps to keep track of the previous landmark
 * proportions relative to the detected face and to interpolate landmark positions for future
 * updates if the landmarks are missing.  This helps to compensate for intermediate frames where the
 * face was detected but one or both of the eyes were not detected.  Missing landmarks can happen
 * during quick movements due to camera image blurring.
 */
public class FaceTracker extends Tracker<Face> {
    private static final float EYE_CLOSED_THRESHOLD = 0.95f; //원래 0.4f

    private GraphicOverlay mOverlay;
    private EyesGraphics mEyesGraphics;

    private Integer rightcount = 0;
    private Boolean rightflag = true;


    /// by kim
    private Queue<Boolean> rightQue = new LinkedList<Boolean>();
    private Integer rightCloseCount = 0;

    private Queue<Boolean> leftQue = new LinkedList<Boolean>();
    private Integer leftCloseCount = 0;


    private static final float CLOSED_THRESHOLD = 0.9f; // TIME_TO_CHECK 번 중에서 감은 개수의 퍼센트
    private static final Integer TIME_TO_CHECK = 30;
    /// by kim


    private Integer leftcount = 0;
    private Boolean leftflag = true;

    // Record the previously seen proportions of the landmark locations relative to the bounding box
    // of the face.  These proportions can be used to approximate where the landmarks are within the
    // face bounding box if the eye landmark is missing in a future update.
    @SuppressLint("UseSparseArrays")
    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

    // Similarly, keep track of the previous eye open state so that it can be reused for
    // intermediate frames which lack eye landmarks and corresponding eye state.
    private boolean mPreviousIsLeftOpen = true;
    private boolean mPreviousIsRightOpen = true;


    //==============================================================================================
    // Methods
    //==============================================================================================

    public FaceTracker(GraphicOverlay overlay) {
        mOverlay = overlay;
    }

    /**
     * Resets the underlying googly eyes graphic and associated physics state.
     */
    @Override
    public void onNewItem(int id, Face face) {
        mEyesGraphics = new EyesGraphics(mOverlay);
    }

    /**
     * Updates the positions and state of eyes to the underlying graphic, according to the most
     * recent face detection results.  The graphic will render the eyes and simulate the motion of
     * the iris based upon these changes over time.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        mOverlay.add(mEyesGraphics);

        updatePreviousProportions(face);

        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);

        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        boolean isLeftOpen;
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isLeftOpen = mPreviousIsLeftOpen;
        } else {
            isLeftOpen = (leftOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsLeftOpen = isLeftOpen;
            if (leftflag == isLeftOpen) { //눈이 열려있을때
                if (leftQue.size() != TIME_TO_CHECK) { // 50개가 아직 채워지지 않았을때
                    leftQue.add(true);
                } else { // 50개 전부 채웠을때
                    if ((leftCloseCount / TIME_TO_CHECK) >= CLOSED_THRESHOLD) { // 50개 다 채웠는데 눈 감은 비율이 95퍼센트 이상이면
                        leftQue.clear(); // 큐를 전부 비우고
                        leftCloseCount = 0; // 값 초기화
                        ((EyeActivity) EyeActivity.mContext).doleftAction();
                    } else { // 50개를 다 채웠는데 눈 감은 비율이 95퍼 이상이 아니면
                        boolean tmp = leftQue.poll();
                        if (!tmp) { // 가장 처음이 false 이면
                            leftCloseCount -= 1; // 닫힌거 개수 하나 빼고
                        }
                        leftQue.add(true);
                    }
                }
            } else { // 눈이 감겨있을때
                if (leftQue.size() != TIME_TO_CHECK) { // 50개가 아직 채워지지 않았을때
                    leftQue.add(false);
                    leftCloseCount += 1;
                } else { // 50개 전부 채웠을때
                    if ((leftCloseCount / TIME_TO_CHECK) >= CLOSED_THRESHOLD) { // 50개 다 채웠는데 눈 감은 비율이 95퍼센트 이상이면
                        leftQue.clear(); // 큐를 전부 비우고
                        leftCloseCount = 0; // 값 초기화
                        ((EyeActivity) EyeActivity.mContext).doleftAction();
                    } else { // 50개를 다 채웠는데 눈 감은 비율이 95퍼 이상이 아니면 개수는 하나빼고 하나 더하니까 변화 없음
                        boolean tmp =  leftQue.poll();
                        if (tmp){
                            leftCloseCount +=1;
                        }
                        leftQue.add(false);
                    }
                }
            }
        }

        float rightOpenScore = face.getIsRightEyeOpenProbability();
        boolean isRightOpen;
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) { // 인식 제대로 안될때
            isRightOpen = mPreviousIsRightOpen;
        } else {
            isRightOpen = (rightOpenScore > EYE_CLOSED_THRESHOLD); // t,f
            mPreviousIsRightOpen = isRightOpen;

            Log.i("오른눈 체크 tf", String.valueOf(isRightOpen));
            Log.i("오른눈 큐 개수", String.valueOf(rightQue.size()));

            if (rightflag == isRightOpen) { //눈이 열려있을때
                if (rightQue.size() != TIME_TO_CHECK) { // 50개가 아직 채워지지 않았을때
                    rightQue.add(true);
                } else { // 50개 전부 채웠을때
                    if ((rightCloseCount / TIME_TO_CHECK) >= CLOSED_THRESHOLD) { // 50개 다 채웠는데 눈 감은 비율이 95퍼센트 이상이면
                        rightQue.clear(); // 큐를 전부 비우고

                        rightCloseCount = 0; // 값 초기화
                        ((EyeActivity) EyeActivity.mContext).dorightAction();
                    } else { // 50개를 다 채웠는데 눈 감은 비율이 95퍼 이상이 아니면
                        boolean tmp = rightQue.poll();
                        if (!tmp) { // 가장 처음이 false 이면
                            rightCloseCount -= 1; // 닫힌거 개수 하나 빼고

                        }
                        rightQue.add(true);
                    }
                }
            } else { // 눈이 감겨있을때
                if (rightQue.size() != TIME_TO_CHECK) { // 50개가 아직 채워지지 않았을때
                    rightQue.add(false);
                    rightCloseCount += 1;
                    Log.i("오른눈 감긴 수 체크 ", String.valueOf(rightCloseCount));
                    Log.i("오른눈 감긴 수 비율 ", String.valueOf((rightCloseCount / TIME_TO_CHECK)));
                } else { // 50개 전부 채웠을때
                    if ((rightCloseCount / TIME_TO_CHECK) >= CLOSED_THRESHOLD) { // 50개 다 채웠는데 눈 감은 비율이 95퍼센트 이상이면
                        rightQue.clear(); // 큐를 전부 비우고
                        rightCloseCount = 0; // 값 초기화
                        ((EyeActivity) EyeActivity.mContext).dorightAction();
                    } else { // 50개를 다 채웠는데 눈 감은 비율이 95퍼 이상이 아니면 개수는 하나빼고 하나 더하니까 변화 없음

                        boolean tmp = rightQue.poll();
                        if (tmp){
                            rightCloseCount +=1;
                        }
                        rightQue.add(false);
                    }
                }
            }
            mEyesGraphics.updateEyes(leftPosition, isLeftOpen, rightPosition, isRightOpen);
//        Toast.makeText(mOverlay.getContext(), (int) rightOpenScore, Toast.LENGTH_SHORT).show(); //추가
        }
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        mOverlay.remove(mEyesGraphics);
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the googly eyes graphic from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mEyesGraphics);
    }

    //==============================================================================================
    // Private
    //==============================================================================================


    private void updatePreviousProportions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            mPreviousProportions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }

    /**
     * Finds a specific landmark position, or approximates the position based on past observations
     * if it is not present.
     */
    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = mPreviousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }
}