package scut.luluteam.gutils.utils.headsmsg;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;


import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import scut.luluteam.gutils.utils.CountTime;

/**
 * 负责显示HeadMsg，并提供动画效果
 * HeadMsg的页面信息
 * <p>
 * Created by guan on 6/23/17.
 */

class HeadMsgView extends LinearLayout {

    private float rawX = 0;
    private float rawY = 0;
    private float touchX = 0;
    private float startY = 0;


    /**
     * 重要：悬浮窗的其他具体view都是加在这个rootview
     */
    //public LinearLayout floatRootView_ly;
    /**
     * view显示的相关参数
     */
    public int originalLeft;
    public int viewWidth;
    private float validWidth;
    private int maxVelocity;

    private VelocityTracker velocityTracker;
    /**
     * HeadMsg的实例
     */
    private HeadsMsg headsMsg;

    /**
     * 负责倒计时，用于自动移出通知
     */
    private CountTime countTime;

    private int preLeft;

    private Context mContext;


    enum ScrollOrientationEnum {
        VERTICAL, HORIZONTAL, NONE
    }

    private ScrollOrientationEnum scrollOrientationEnum = ScrollOrientationEnum.NONE;


    /**
     * 构造函数
     *
     * @param context
     */
    public HeadMsgView(Context context) {
        super(context);
        this.mContext = context;
//        LinearLayout view = (LinearLayout) LayoutInflater.from(getContext()).
//                inflate(R.layout.view_notification_bg, null);
        //floatRootView_ly = (LinearLayout) view.findViewById(R.id.floatRootView_ly);

        /**
         * 设置相关默认参数
         */
        maxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        viewWidth = context.getResources().getDisplayMetrics().widthPixels;
        validWidth = viewWidth / 2.0f;
        originalLeft = 0;


    }

    private int pointerId;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        rawX = event.getRawX();
        rawY = event.getRawY();
        acquireVelocityTracker(event);
        //cutDown = headsMsg.getDuration();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                startY = event.getRawY();
                pointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                switch (scrollOrientationEnum) {
                    case NONE:
                        if (Math.abs((rawX - touchX)) > 20) {
                            scrollOrientationEnum = ScrollOrientationEnum.HORIZONTAL;

                        } else if (startY - rawY > 20) {
                            scrollOrientationEnum = ScrollOrientationEnum.VERTICAL;

                        }

                        break;
                    case HORIZONTAL:
                        updatePosition((int) (rawX - touchX));
                        break;
                    case VERTICAL:
                        if (startY - rawY > 20) {
                            dismiss(true);
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000, maxVelocity);
                int dis = (int) velocityTracker.getYVelocity(pointerId);
                if (scrollOrientationEnum == ScrollOrientationEnum.NONE) {
                    if (headsMsg.getNotification().contentIntent != null) {

                        try {
                            headsMsg.getNotification().contentIntent.send();
                            dismiss(true);
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }

                int toX;
                if (preLeft > 0) {
                    toX = (int) (preLeft + Math.abs(dis));
                } else {
                    toX = (int) (preLeft - Math.abs(dis));
                }
                if (toX <= -validWidth) {
                    float preAlpha = 1 - Math.abs(preLeft) / validWidth;
                    preAlpha = preAlpha >= 0 ? preAlpha : 0;
                    translationX(preLeft, -(validWidth + 10), preAlpha, 0);
                } else if (toX <= validWidth) {
                    float preAlpha = 1 - Math.abs(preLeft) / validWidth;
                    preAlpha = preAlpha >= 0 ? preAlpha : 0;
                    translationX(preLeft, 0, preAlpha, 1);
                } else {
                    float preAlpha = 1 - Math.abs(preLeft) / validWidth;
                    preAlpha = preAlpha >= 0 ? preAlpha : 0;
                    translationX(preLeft, validWidth + 10, preAlpha, 0);
                }
                preLeft = 0;
                scrollOrientationEnum = ScrollOrientationEnum.NONE;
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * @param event 向VelocityTracker添加MotionEvent
     * @see VelocityTracker#obtain()
     * @see VelocityTracker#addMovement(MotionEvent)
     */
    private void acquireVelocityTracker(MotionEvent event) {
        if (null == velocityTracker) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }


    private void updatePosition(int left) {
        float preAlpha = 1 - Math.abs(preLeft) / validWidth;
        float leftAlpha = 1 - Math.abs(left) / validWidth;
        preAlpha = preAlpha >= 0 ? preAlpha : 0;
        leftAlpha = leftAlpha >= 0 ? leftAlpha : 0;
        translationX(preLeft, left, preAlpha, leftAlpha);

        preLeft = left;
    }

    /**
     * 手指左右滑动，取消HeadMsg
     *
     * @param fromX
     * @param toX
     * @param formAlpha
     * @param toAlpha
     */
    private void translationX(float fromX, float toX, float formAlpha, final float toAlpha) {
        ObjectAnimator a1 = ObjectAnimator.ofFloat(headsMsg.getCustomView(), "alpha", formAlpha, toAlpha);
        ObjectAnimator a2 = ObjectAnimator.ofFloat(headsMsg.getCustomView(), "translationX", fromX, toX);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(a1, a2);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (toAlpha == 0) {
                    dismiss(false);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.start();
    }

    /**
     * 显示悬浮窗的动画
     */
    public void showWithAnim() {
        if (this.getParent() != null) {
            /**
             * 为悬浮窗添加动画
             */
            ObjectAnimator a = ObjectAnimator.ofFloat(headsMsg.getCustomView(), "translationY", -700, 0);
            a.setDuration(600);
            a.start();
        }

    }


    /**
     * 取消悬浮窗的：显示相关动画，清理相关资源
     */
    private void dismiss(boolean withAnim) {
        if (withAnim) {
            //HeadMsgManager.getInstant(getContext()).dismissWithAnim();

            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    ObjectAnimator anim = ObjectAnimator.ofFloat(headsMsg.getCustomView(), "translationY", 0, -700);
                    anim.setDuration(700);
                    anim.start();

                    anim.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            HeadMsgManager.getInstant().dismiss();
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });


                }
            });

        } else {
            HeadMsgManager.getInstant().dismiss();
        }

        if (countTime != null) {
            countTime.stop();
        }
        if (velocityTracker != null) {
            try {
                velocityTracker.clear();
                velocityTracker.recycle();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将HeadMsg和HeadMsgView绑定
     *
     * @param headsMsg
     */
    public void setUpWithHeadMsg(final HeadsMsg headsMsg) {
        this.headsMsg = headsMsg;
//        View customView;
//        if ((customView = headsMsg.getCustomView()) != null) {
//            this.floatRootView_ly.addView(customView);
//        } else {
//            System.out.println("HeadMsg缺少CustomView");
//            return;
//        }

        this.addView(headsMsg.getCustomView());


        if (!headsMsg.isSticky()) {
            countTime = CountTime.newInstance((int) headsMsg.getDuration(),
                    new CountTime.CountTimeEvent() {
                        @Override
                        public void timeOut() {
                            dismiss(true);
                        }
                    });
            countTime.start();
        }


    }


}