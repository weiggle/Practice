package com.liwei.custom;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.example.weili.practice.R;

/**
 * Created by wei.li on 2016/3/15.
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class RangeSeekbar extends View {

    private static final String DEBUG_TAG = "RangeSeekbar.java";

        private static final int DEFAULT_DURATION = 100;

        private enum DIRECTION {
            LEFT, RIGHT;
    }

    private int mDuration;

    /**
     * 左右滑块的滑动支持
     */
    private Scroller mLeftScroller;
    private Scroller mRightScroller;

    /**
     * 左右滑块的背景图片
     */
    private Drawable mLeftCursorBG;
    private Drawable mRightCursorBG;

    private int[] mPressedEnableState = new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled };
    private int[] mUnPresseEanabledState = new int[] { -android.R.attr.state_pressed, android.R.attr.state_enabled };

    /**
     * 颜色区分
     */
    private int mTextColorNormal;
    private int mTextColorSelected;
    private int mSeekbarColorNormal;
    private int mSeekbarColorSelected;

    /**
     * 滑动控件高度
     */
    private int mSeekbarHeight;

    /**
     * 文字标注的字体大小
     */
    private int mTextSize;

    /**
     * 文字与滑块之间的距离
     */
    private int mMarginBetween;

    /**
     * Length of every part. As we divide some parts according to marks.
     */
    private int mPartLength;

    /**
     * 标尺的文字内容
     */
    private CharSequence[] mTextArray;

    /**
     *
     */
    private float[] mTextWidthArray;

    private Rect mPaddingRect;
    private Rect mLeftCursorRect;
    private Rect mRightCursorRect;

    private RectF mSeekbarRect;
    private RectF mSeekbarRectSelected;

    private float mLeftCursorIndex = 0;
    private float mRightCursorIndex = 1.0f;
    private int mLeftCursorNextIndex = 0;
    private int mRightCursorNextIndex = 1;

    private Paint mPaint;

    private int mLeftPointerLastX;
    private int mRightPointerLastX;

    private int mLeftPointerID = -1;
    private int mRightPointerID = -1;

    private boolean mLeftHited;
    private boolean mRightHited;

    private int mRightBoundary;

    private OnCursorChangeListener mListener;

    private Rect[] mClickRectArray;
    private int mClickIndex = -1;
    private int mClickDownLastX = -1;
    private int mClickDownLastY = -1;

    private String leftText;
    private String rightText;

    public RangeSeekbar(Context context) {
        this(context, null, 0);
    }

    public RangeSeekbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyConfig(context, attrs);

        if (mPaddingRect == null) {
            mPaddingRect = new Rect();
        }
        mPaddingRect.left = getPaddingLeft();
        mPaddingRect.top = getPaddingTop();
        mPaddingRect.right = getPaddingRight();
        mPaddingRect.bottom = getPaddingBottom();

        mLeftCursorRect = new Rect();
        mRightCursorRect = new Rect();

        mSeekbarRect = new RectF();
        mSeekbarRectSelected = new RectF();

        if (mTextArray != null) {
            mTextWidthArray = new float[mTextArray.length];
            mClickRectArray = new Rect[mTextArray.length];
        }

        mLeftScroller = new Scroller(context, new DecelerateInterpolator());
        mRightScroller = new Scroller(context, new DecelerateInterpolator());

        initPaint();
        initTextWidthArray();

        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);
    }

    private void applyConfig(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekbar);

        mDuration = a.getInteger(R.styleable.RangeSeekbar_autoMoveDuration, DEFAULT_DURATION);

        mLeftCursorBG = a.getDrawable(R.styleable.RangeSeekbar_leftCursorBackground);
        mRightCursorBG = a.getDrawable(R.styleable.RangeSeekbar_rightCursorBackground);

        mTextColorNormal = a.getColor(R.styleable.RangeSeekbar_textColorNormal, Color.BLACK);
        mTextColorSelected = a.getColor(R.styleable.RangeSeekbar_textColorSelected, Color.rgb(242, 79, 115));

        mSeekbarColorNormal = a.getColor(R.styleable.RangeSeekbar_seekbarColorNormal, Color.rgb(218, 215, 215));
        mSeekbarColorSelected = a.getColor(R.styleable.RangeSeekbar_seekbarColorSelected, Color.rgb(242, 79, 115));

        mSeekbarHeight = (int) a.getDimension(R.styleable.RangeSeekbar_seekbarHeight, 10);
        mTextSize = (int) a.getDimension(R.styleable.RangeSeekbar_textSize, 15);
        mMarginBetween = (int) a.getDimension(R.styleable.RangeSeekbar_spaceBetween, 15);

        mTextArray = a.getTextArray(R.styleable.RangeSeekbar_markTextArray);
        if (mTextArray != null && mTextArray.length > 0) {
            mLeftCursorIndex = 0;
            // mRightCursorIndex = mTextArray.length - 1;
            mRightCursorIndex = mTextArray.length/2; // 指定到价格为500的位置
            mRightCursorNextIndex = (int) mRightCursorIndex;
            leftText = mTextArray[(int)mLeftCursorIndex].toString();
            rightText = mTextArray[mRightCursorNextIndex].toString();
        }

        a.recycle();
    }

    /**
     * 设置初始化选择位置
     */
    public void setCursorInitPosition(float leftCursorIndex, float rightCursorIndex) {
        mLeftCursorIndex = leftCursorIndex;
        mRightCursorIndex = rightCursorIndex;
        leftText = mTextArray[(int)mLeftCursorIndex].toString();
        rightText = mTextArray[(int)mRightCursorIndex].toString();
        invalidate();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mTextSize);
    }

    private void initTextWidthArray() {
        if (mTextArray != null && mTextArray.length > 0) {
            final int length = mTextArray.length;
            for (int i = 0; i < length; i++) {
                mTextWidthArray[i] = mPaint.measureText(mTextArray[i].toString());
            }
        }

    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);

        if (mPaddingRect == null) {
            mPaddingRect = new Rect();
        }
        mPaddingRect.left = left;
        mPaddingRect.top = top;
        mPaddingRect.right = right;
        mPaddingRect.bottom = bottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        final int leftPointerH = mLeftCursorBG.getIntrinsicHeight();
        final int rightPointerH = mRightCursorBG.getIntrinsicHeight();

        // Get max height between left and right cursor.
        final int maxOfCursor = Math.max(leftPointerH, rightPointerH);
        // Then get max height between seekbar and cursor.
        final int maxOfCursorAndSeekbar = Math.max(mSeekbarHeight, maxOfCursor);
        // So we get the needed height.
        int heightNeeded = maxOfCursorAndSeekbar + mMarginBetween + mTextSize + mPaddingRect.top + mPaddingRect.bottom;

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(heightSize, heightNeeded), MeasureSpec.EXACTLY);

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        mSeekbarRect.left = mPaddingRect.left + mLeftCursorBG.getIntrinsicWidth() / 2;
        mSeekbarRect.right = widthSize - mPaddingRect.right - mRightCursorBG.getIntrinsicWidth() / 2;
        mSeekbarRect.top = mPaddingRect.top + mTextSize + mMarginBetween;
        mSeekbarRect.bottom = mSeekbarRect.top + mSeekbarHeight;

        mSeekbarRectSelected.top = mSeekbarRect.top;
        mSeekbarRectSelected.bottom = mSeekbarRect.bottom;

        mPartLength = ((int) (mSeekbarRect.right - mSeekbarRect.left)) / (mTextArray.length - 1);

        mRightBoundary = (int) (mSeekbarRect.right + mRightCursorBG.getIntrinsicWidth() / 2);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /*** Draw text marks ***/
        final int length = mTextArray.length;
        /*** Draw seekbar ***/
        final float radius = (float) mSeekbarHeight / 2;
        mSeekbarRectSelected.left = mSeekbarRect.left + mPartLength * mLeftCursorIndex;
        mSeekbarRectSelected.right = mSeekbarRect.left + mPartLength * mRightCursorIndex;
        // If whole of seekbar is selected, just draw seekbar with selected
        // color.
        if (mLeftCursorIndex == 0 && mRightCursorIndex == length - 1) {
            mPaint.setColor(mSeekbarColorSelected);
            canvas.drawRoundRect(mSeekbarRect, radius, radius, mPaint);
        } else {
            // Draw background first.
            mPaint.setColor(mSeekbarColorNormal);
            canvas.drawRoundRect(mSeekbarRect, radius, radius, mPaint);

            // Draw selected part.
            mPaint.setColor(mSeekbarColorSelected);
            // Can draw rounded rectangle, but original rectangle is enough.
            // Because edges of selected part will be covered by cursors.
            canvas.drawRect(mSeekbarRectSelected, mPaint);
        }

        /*** Draw cursors ***/
        // left cursor first
        final int leftWidth = mLeftCursorBG.getIntrinsicWidth();
        final int leftHieght = mLeftCursorBG.getIntrinsicHeight();
        final int leftLeft = (int) (mSeekbarRectSelected.left - (float) leftWidth / 2);
        final int leftTop = (int) ((mSeekbarRect.top + mSeekbarHeight / 2) - (leftHieght / 2));
        mLeftCursorRect.left = leftLeft;
        mLeftCursorRect.top = leftTop;
        // mLeftCursorRect.top = 0;
        mLeftCursorRect.right = leftLeft + leftWidth;
        mLeftCursorRect.bottom = leftTop + leftHieght;
        // mLeftCursorRect.bottom = leftHieght;
        mLeftCursorBG.setBounds(mLeftCursorRect);
        mLeftCursorBG.draw(canvas);

        // right cursor second
        final int rightWidth = mRightCursorBG.getIntrinsicWidth();
        final int rightHeight = mRightCursorBG.getIntrinsicHeight();
        final int rightLeft = (int) (mSeekbarRectSelected.right - (float) rightWidth / 2);
        final int rightTop = (int) ((mSeekbarRectSelected.top + mSeekbarHeight / 2) - (rightHeight / 2));
        mRightCursorRect.left = rightLeft;
        mRightCursorRect.top = rightTop;
        // mRightCursorRect.top = 0;
        mRightCursorRect.right = rightLeft + rightWidth;
        mRightCursorRect.bottom = rightTop + rightHeight;
        // mRightCursorRect.bottom = rightHeight;
        mRightCursorBG.setBounds(mRightCursorRect);
        mRightCursorBG.draw(canvas);

        // 把原来绘制的文字内容移动到下方
        mPaint.setTextSize(mTextSize);

        canvas.drawText("开始："+leftText, (mSeekbarRect.width()/2 - mTextWidthArray[0]) / 2, mPaddingRect.top  + mTextSize, mPaint);
        canvas.drawText("抵达："+rightText, (mSeekbarRect.width()/2 + mTextWidthArray[0]/2) , mPaddingRect.top  + mTextSize, mPaint);
        for (int i = 0; i < length; i++) {
            if ((i > mLeftCursorIndex && i < mRightCursorIndex) || (i == mLeftCursorIndex || i == mRightCursorIndex)) {
                // mPaint.setFakeBoldText(true);
                mPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                mPaint.setColor(mTextColorSelected);
            } else {
                mPaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                mPaint.setColor(mTextColorNormal);
            }

            final String text2draw = mTextArray[i].toString();
            final float textWidth = mTextWidthArray[i];

            float textDrawLeft = 0;
            // The last text mark's draw location should be adjust.
//			if (i == length - 1) {
//				textDrawLeft = mSeekbarRect.right + (mRightCursorBG.getIntrinsicWidth() / 2) - textWidth;
//			} else {
//				textDrawLeft = mSeekbarRect.left + i * mPartLength - textWidth / 2;
//			}

            if (i == length) {
                textDrawLeft = mSeekbarRect.right + (mRightCursorBG.getIntrinsicWidth() / 2) - textWidth;
            } else {
                textDrawLeft = mSeekbarRect.left + i * mPartLength - textWidth / 2;
            }

            // canvas.drawText(text2draw, textDrawLeft, mPaddingRect.top +
            // mTextSize, mPaint);
            // 画下方数轴


//            canvas.drawText(".", textDrawLeft + textWidth / 2, mPaddingRect.top + rightHeight + mTextSize, mPaint);
//            canvas.drawText(text2draw, textDrawLeft, mPaddingRect.top + mMarginBetween + rightHeight + mTextSize,
//                    mPaint);

            Rect rect = mClickRectArray[i];
            if (rect == null) {
                rect = new Rect();
                rect.top = mPaddingRect.top;
                rect.bottom = rect.top + mTextSize + mMarginBetween + mSeekbarHeight;
                rect.left = (int) textDrawLeft;
                rect.right = (int) (rect.left + textWidth);

                mClickRectArray[i] = rect;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        // For multiple touch
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                handleTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                handleTouchUp(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                handleTouchUp(event);
                mClickIndex = -1;
                mClickDownLastX = -1;
                mClickDownLastY = -1;

                break;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int downX = (int) event.getX(actionIndex);
        final int downY = (int) event.getY(actionIndex);

        if (mLeftCursorRect.contains(downX, downY)) {
            if (mLeftHited) {
                return;
            }

            // If hit, change state of drawable, and record id of touch pointer.
            mLeftPointerLastX = downX;
            mLeftCursorBG.setState(mPressedEnableState);
            mLeftPointerID = event.getPointerId(actionIndex);
            mLeftHited = true;

            invalidate();
        } else if (mRightCursorRect.contains(downX, downY)) {
            if (mRightHited) {
                return;
            }

            mRightPointerLastX = downX;
            mRightCursorBG.setState(mPressedEnableState);
            mRightPointerID = event.getPointerId(actionIndex);
            mRightHited = true;

            invalidate();
        } else {
            // If touch x-y not be contained in cursor,
            // then we check if it in click areas
            final int clickBoundaryTop = mClickRectArray[0].top;
            final int clickBoundaryBottom = mClickRectArray[0].bottom;
            mClickDownLastX = downX;
            mClickDownLastY = downY;

            // Step one : if in boundary of total Y.
            if (downY < clickBoundaryTop || downY > clickBoundaryBottom) {
                mClickIndex = -1;
                return;
            }

            // Step two: find nearest mark in x-axis
            final int partIndex = (int) ((downX - mSeekbarRect.left) / mPartLength);
            final int partDelta = (int) ((downX - mSeekbarRect.left) % mPartLength);
            if (partDelta < mPartLength / 2) {
                mClickIndex = partIndex;
            } else if (partDelta > mPartLength / 2) {
                mClickIndex = partIndex + 1;
            }

            if (mClickIndex == mLeftCursorIndex || mClickIndex == mRightCursorIndex) {
                mClickIndex = -1;
                return;
            }

            // Step three: check contain
            if (!mClickRectArray[mClickIndex].contains(downX, downY)) {
                mClickIndex = -1;
            }
        }
    }

    private void handleTouchUp(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int actionID = event.getPointerId(actionIndex);

        if (actionID == mLeftPointerID) {
            if (!mLeftHited) {
                return;
            }

            // If cursor between in tow mark locations, it should be located on
            // the lower or higher one.

            // step 1:Calculate the offset with lower mark.
            final int lower = (int) Math.floor(mLeftCursorIndex);
            final int higher = (int) Math.ceil(mLeftCursorIndex);

            final float offset = mLeftCursorIndex - lower;
            //if (offset != 0) {

            // step 2:Decide which mark will go to.
            if (offset < 0.5f) {
                // If left cursor want to be located on lower mark, go ahead
                // guys.
                // Because right cursor will never appear lower than the
                // left one.
                mLeftCursorNextIndex = lower;
            } else if (offset > 0.5f) {
                mLeftCursorNextIndex = higher;
                // If left cursor want to be located on higher mark,
                // situation becomes a little complicated.
                // We should check that whether distance between left and
                // right cursor is less than 1, and next index of left
                // cursor is difference with current
                // of right one.
                if (Math.abs(mLeftCursorIndex - mRightCursorIndex) <= 1
                        && mLeftCursorNextIndex == mRightCursorNextIndex) {
                    // Left can not go to the higher, just to the lower one.
                    mLeftCursorNextIndex = lower;
                }
            }

            // step 3: Move to.
            if (!mLeftScroller.computeScrollOffset()) {
                final int fromX = (int) (mLeftCursorIndex * mPartLength);

                mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex * mPartLength - fromX, 0, mDuration);
                leftText = mTextArray[mLeftCursorNextIndex].toString();
                triggleCallback(true, mLeftCursorNextIndex);
            }
            //}

            // Reset values of parameters
            mLeftPointerLastX = 0;
            mLeftCursorBG.setState(mUnPresseEanabledState);
            mLeftPointerID = -1;
            mLeftHited = false;

            invalidate();
        } else if (actionID == mRightPointerID) {
            if (!mRightHited) {
                return;
            }

            final int lower = (int) Math.floor(mRightCursorIndex);
            final int higher = (int) Math.ceil(mRightCursorIndex);

            final float offset = mRightCursorIndex - lower;
            if (offset != 0) {

                if (offset > 0.5f) {
                    mRightCursorNextIndex = higher;
                } else if (offset < 0.5f) {
                    mRightCursorNextIndex = lower;
                    if (Math.abs(mLeftCursorIndex - mRightCursorIndex) <= 1
                            && mRightCursorNextIndex == mLeftCursorNextIndex) {
                        mRightCursorNextIndex = higher;
                    }
                }

                if (!mRightScroller.computeScrollOffset()) {
                    final int fromX = (int) (mRightCursorIndex * mPartLength);

                    mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex * mPartLength - fromX, 0, mDuration);
                    rightText = mTextArray[mRightCursorNextIndex].toString();
                    triggleCallback(false, mRightCursorNextIndex);
                }
            }

            mRightPointerLastX = 0;
            mLeftCursorBG.setState(mUnPresseEanabledState);
            mRightPointerID = -1;
            mRightHited = false;

            invalidate();
        } else {
            final int pointerIndex = event.findPointerIndex(actionID);
            final int upX = (int) event.getX(pointerIndex);
            final int upY = (int) event.getY(pointerIndex);

            if (mClickIndex != -1 && mClickRectArray[mClickIndex].contains(upX, upY)) {
                // Find nearest cursor
                final float distance2LeftCursor = Math.abs(mLeftCursorIndex - mClickIndex);
                final float distance2Right = Math.abs(mRightCursorIndex - mClickIndex);

                final boolean moveLeft = distance2LeftCursor <= distance2Right;
                int fromX = 0;
                if (moveLeft) {
                    if (!mLeftScroller.computeScrollOffset()) {
                        mLeftCursorNextIndex = mClickIndex;
                        fromX = (int) (mLeftCursorIndex * mPartLength);
                        mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex * mPartLength - fromX, 0, mDuration);
                        leftText = mTextArray[mLeftCursorNextIndex].toString();
                        triggleCallback(true, mLeftCursorNextIndex);

                        invalidate();
                    }
                } else {
                    if (!mRightScroller.computeScrollOffset()) {
                        mRightCursorNextIndex = mClickIndex;
                        fromX = (int) (mRightCursorIndex * mPartLength);
                        mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex * mPartLength - fromX, 0, mDuration);
                        rightText = mTextArray[mRightCursorNextIndex].toString();
                        triggleCallback(false, mRightCursorNextIndex);

                        invalidate();
                    }
                }
            }
        }
    }

    private void handleTouchMove(MotionEvent event) {
        if (mClickIndex != -1) {
            final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int x = (int) event.getX(actionIndex);
            final int y = (int) event.getY(actionIndex);

            if (!mClickRectArray[mClickIndex].contains(x, y)) {
                mClickIndex = -1;
            }
        }

        if (mLeftHited && mLeftPointerID != -1) {

            final int index = event.findPointerIndex(mLeftPointerID);
            final float x = event.getX(index);

            float deltaX = x - mLeftPointerLastX;
            mLeftPointerLastX = (int) x;

            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT : DIRECTION.RIGHT);

            if (direction == DIRECTION.LEFT && mLeftCursorIndex == 0) {
                return;
            }

            // Check whether cursor will move out of boundary
            if (mLeftCursorRect.left + deltaX < mPaddingRect.left) {
                mLeftCursorIndex = 0;
                leftText= mTextArray[0].toString();
                invalidate();
                return;
            }

            // Check whether left and right cursor will collision.
            if (mLeftCursorRect.right + deltaX > mRightCursorRect.right) {
                // Check whether right cursor is in "Touch" mode( if in touch
                // mode, represent that we can not move it at all), or right
                // cursor reach the boundary.
                if (mRightHited || mRightCursorIndex == mTextArray.length - 1 || mRightScroller.computeScrollOffset()) {
                    // Just move left cursor to the left side of right one.
                    deltaX = mRightCursorRect.left - mLeftCursorRect.right;
                } else {
                    // Move right cursor to higher location.
                    final int maxMarkIndex = mTextArray.length - 1;

                    if (mRightCursorIndex <= maxMarkIndex - 1) {
                        mRightCursorNextIndex = (int) (mRightCursorIndex + 1);

                        if (!mRightScroller.computeScrollOffset()) {
                            final int fromX = (int) (mRightCursorIndex * mPartLength);

                            mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex * mPartLength - fromX, 0,
                                    mDuration);
                            rightText = mTextArray[mRightCursorNextIndex].toString();
                            triggleCallback(false, mRightCursorNextIndex);
                        }
                    }
                }
            }

            // After some calculate, if deltaX is still be zero, do quick
            // return.
            if (deltaX == 0) {
                return;
            }

            // Calculate the movement.
            final float moveX = deltaX / mPartLength;
            mLeftCursorIndex += moveX;

            invalidate();
        }

        if (mRightHited && mRightPointerID != -1) {

            final int index = event.findPointerIndex(mRightPointerID);
            final float x = event.getX(index);

            float deltaX = x - mRightPointerLastX;
            mRightPointerLastX = (int) x;

            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT : DIRECTION.RIGHT);

            final int maxIndex = mTextArray.length - 1;
            if (direction == DIRECTION.RIGHT && mRightCursorIndex == maxIndex) {
                rightText = mTextArray[mTextArray.length-1].toString();
                invalidate();
                return;
            }

            if (mRightCursorRect.right + deltaX > mRightBoundary) {
                deltaX = mRightBoundary - mRightCursorRect.right;
            }

            final int maxMarkIndex = mTextArray.length - 1;
            if (direction == DIRECTION.RIGHT && mRightCursorIndex == maxMarkIndex) {
                rightText = mTextArray[mTextArray.length-1].toString();
                invalidate();
                return;
            }

            if (mRightCursorRect.left + deltaX < mLeftCursorRect.left) {
                if (mLeftHited || mLeftCursorIndex == 0 || mLeftScroller.computeScrollOffset()) {
                    deltaX = mLeftCursorRect.left - mRightCursorRect.left;
                } else {
                    if (mLeftCursorIndex >= 1) {
                        mLeftCursorNextIndex = (int) (mLeftCursorIndex - 1);

                        if (!mLeftScroller.computeScrollOffset()) {
                            final int fromX = (int) (mLeftCursorIndex * mPartLength);
                            mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex * mPartLength - fromX, 0,
                                    mDuration);
                            leftText = mTextArray[mLeftCursorNextIndex].toString();
                            triggleCallback(true, mLeftCursorNextIndex);
                        }
                    }
                }
            }

            if (deltaX == 0) {
                return;
            }

            final float moveX = deltaX / mPartLength;
            mRightCursorIndex += moveX;

            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        if (mLeftScroller.computeScrollOffset()) {
            final int deltaX = mLeftScroller.getCurrX();

            mLeftCursorIndex = (float) deltaX / mPartLength;
            leftText = mTextArray[(int)mLeftCursorIndex].toString();
            invalidate();
        }

        if (mRightScroller.computeScrollOffset()) {
            final int deltaX = mRightScroller.getCurrX();

            mRightCursorIndex = (float) deltaX / mPartLength;
            rightText = mTextArray[(int)mRightCursorIndex].toString();
            invalidate();
        }
    }

    /**
     * 触发选择后的回调
     * @param isLeft
     * @param location
     */
    private void triggleCallback(boolean isLeft, int location) {
        if (mListener == null) {
            return;
        }

        if (isLeft) {
            mListener.onLeftCursorChanged(location, mTextArray[location].toString());
        } else {
            mListener.onRightCursorChanged(location, mTextArray[location].toString());
        }
    }

    public void setLeftSelection(int partIndex) {
        if (partIndex >= mTextArray.length - 1 || partIndex <= 0) {
            throw new IllegalArgumentException("Index should from 0 to size of text array minus 2!");
        }

        if (partIndex != mLeftCursorIndex) {
            if (!mLeftScroller.isFinished()) {
                mLeftScroller.abortAnimation();
            }
            mLeftCursorNextIndex = partIndex;
            final int leftFromX = (int) (mLeftCursorIndex * mPartLength);
            mLeftScroller.startScroll(leftFromX, 0, mLeftCursorNextIndex * mPartLength - leftFromX, 0, mDuration);
            leftText  = mTextArray[mLeftCursorNextIndex].toString();
            invalidate();
            triggleCallback(true, mLeftCursorNextIndex);

            if (mRightCursorIndex <= mLeftCursorNextIndex) {
                if (!mRightScroller.isFinished()) {
                    mRightScroller.abortAnimation();
                }
                mRightCursorNextIndex = mLeftCursorNextIndex + 1;
                final int rightFromX = (int) (mRightCursorIndex * mPartLength);
                mRightScroller.startScroll(rightFromX, 0, mRightCursorNextIndex * mPartLength - rightFromX, 0,
                        mDuration);
                rightText  = mTextArray[mRightCursorNextIndex].toString();
                triggleCallback(false, mRightCursorNextIndex);
            }

            invalidate();
        }
    }

    public void setRightSelection(int partIndex) {
        if (partIndex >= mTextArray.length || partIndex <= 0) {
            throw new IllegalArgumentException("Index should from 1 to size of text array minus 1!");
        }

        if (partIndex != mRightCursorIndex) {
            if (!mRightScroller.isFinished()) {
                mRightScroller.abortAnimation();
            }

            mRightCursorNextIndex = partIndex;
            final int rightFromX = (int) (mPartLength * mRightCursorIndex);
            mRightScroller.startScroll(rightFromX, 0, mRightCursorNextIndex * mPartLength - rightFromX, 0, mDuration);
            rightText  = mTextArray[mRightCursorNextIndex].toString();
            triggleCallback(false, mRightCursorNextIndex);

            if (mLeftCursorIndex >= mRightCursorNextIndex) {
                if (!mLeftScroller.isFinished()) {
                    mLeftScroller.abortAnimation();
                }

                mLeftCursorNextIndex = mRightCursorNextIndex - 1;
                final int leftFromX = (int) (mLeftCursorIndex * mPartLength);
                mLeftScroller.startScroll(leftFromX, 0, mLeftCursorNextIndex * mPartLength - leftFromX, 0, mDuration);
                leftText  = mTextArray[mLeftCursorNextIndex].toString();
                triggleCallback(true, mLeftCursorNextIndex);
            }
            invalidate();
        }
    }

    public void setLeftCursorBackground(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Do you want to make left cursor invisible?");
        }

        mLeftCursorBG = drawable;

        requestLayout();
        invalidate();
    }

    public void setLeftCursorBackground(int resID) {
        if (resID < 0) {
            throw new IllegalArgumentException("Do you want to make left cursor invisible?");
        }

        mLeftCursorBG = getResources().getDrawable(resID);

        requestLayout();
        invalidate();
    }

    public void setRightCursorBackground(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Do you want to make right cursor invisible?");
        }

        mRightCursorBG = drawable;

        requestLayout();
        invalidate();
    }

    public void setRightCursorBackground(int resID) {
        if (resID < 0) {
            throw new IllegalArgumentException("Do you want to make right cursor invisible?");
        }

        mRightCursorBG = getResources().getDrawable(resID);

        requestLayout();
        invalidate();
    }

    public void setTextMarkColorNormal(int color) {
        if (color == Color.TRANSPARENT) {
            throw new IllegalArgumentException("Do you want to make text mark invisible?");
        }

        mTextColorNormal = color;

        invalidate();
    }

    public void setTextMarkColorSelected(int color) {
        if (color == Color.TRANSPARENT) {
            throw new IllegalArgumentException("Do you want to make text mark invisible?");
        }

        mTextColorSelected = color;

        invalidate();
    }

    public void setSeekbarColorNormal(int color) {
        if (color == Color.TRANSPARENT) {
            throw new IllegalArgumentException("Do you want to make seekbar invisible?");
        }

        mSeekbarColorNormal = color;

        invalidate();
    }

    public void setSeekbarColorSelected(int color) {
        if (color <= 0 || color == Color.TRANSPARENT) {
            throw new IllegalArgumentException("Do you want to make seekbar invisible?");
        }

        mSeekbarColorSelected = color;

        invalidate();
    }

    /**
     * In pixels. Users should call this method before view is added to parent.
     *
     * @param height
     */
    public void setSeekbarHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height of seekbar can not less than 0!");
        }

        mSeekbarHeight = height;
    }

    /**
     * To set space between text mark and seekbar.
     *
     * @param space
     */
    public void setSpaceBetween(int space) {
        if (space < 0) {
            throw new IllegalArgumentException("Space between text mark and seekbar can not less than 0!");
        }

        mMarginBetween = space;

        requestLayout();
        invalidate();
    }

    /**
     * This method should be called after {@link #setTextMarkSize(int)}, because
     * view will measure size of text mark by paint.
     *
     * @param
     */
    public void setTextMarks(CharSequence... marks) {
        if (marks == null || marks.length == 0) {
            throw new IllegalArgumentException("Text array is null, how can i do...");
        }

        mTextArray = marks;
        mLeftCursorIndex = 0;
        mRightCursorIndex = mTextArray.length - 1;
        mRightCursorNextIndex = (int) mRightCursorIndex;
        mTextWidthArray = new float[marks.length];
        mClickRectArray = new Rect[mTextArray.length];
        initTextWidthArray();

        requestLayout();
        invalidate();
    }

    /**
     * Users should call this method before view is added to parent.
     *
     * @param size
     *            in pixels
     */
    public void setTextMarkSize(int size) {
        if (size < 0) {
            return;
        }

        mTextSize = size;
        mPaint.setTextSize(size);
    }

    public int getLeftCursorIndex() {
        return (int) mLeftCursorIndex;
    }

    public int getRightCursorIndex() {
        return (int) mRightCursorIndex;
    }

    public void setOnCursorChangeListener(OnCursorChangeListener l) {
        mListener = l;
    }

    public interface    OnCursorChangeListener {
        void onLeftCursorChanged(int location, String textMark);

        void onRightCursorChanged(int location, String textMark);
    }
}