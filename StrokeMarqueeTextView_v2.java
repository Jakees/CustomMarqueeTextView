//OSK
package dagger_test.sql.com.daggersample.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import dagger_test.sql.com.daggersample.R;
/**
 * Created by shengqili on 2015/8/26.
 */

/**
 *
 * 此种绘制方式会在主线程中造成 doing too much work in UI Thread，需改进。
 * 参数 :
 *   marqueeStyle取值范围
 *      1:StrokeMarqueeTextView.STYLE_ONE --来回滚动
 *      2:StrokeMarqueeTextView.STYLE_TWO --头尾相接，长度小于TextViewWidth停止滚动
 *      3.DEF_MARQUEEN_STYLE_TWO_SPACE    --是在style2下文本之间的空格距离
 *
 * 1.设置此控件marquee文本使用以下方式-->
 *          setMarqueeText(CharSequence text) 替代 setText(CharSequence text)
 *
 * 2.在布局中设置以下必要属性
 *          android:ellipsize="marquee"
 *          android:singleLine="true"
 *
 * 3.在无法显示的情况下需要在调用此控件的父级View里设置以下两个属性
 *          android:paddingLeft="0dp"
 *          android:paddingRight="0dp"
 * */

 public class StrokeMarqueeTextView extends TextView implements Runnable{
    private boolean isShortMarquee;//如果是短文本，2号Style需调整运行方式
    private boolean isFirstTime;    //获取文本长度计算时间
    private boolean isGetTextWidth;//判断是否已获取文本长度
    private boolean pause;
    private boolean changedFromStyleTwo;
    private boolean isCheckText;
    /**
     * 参数marqueeStyle
     * 1:来回滚动
     * 2:头尾相接，长度小于TextViewWidth停止滚动
     */
    //默认属性值
    public static final int STYLE_ONE = 1;
    public static final int STYLE_TWO = 2;
    private static final int STYLE_THREE = 3;

    private static final int DEF_BORDER_WIDTH = 4;
    private static final int DEF_SCROLL_X = 5;     //每次移动的像素
    private static final int DEF_BORDER_COLOR = -39360;
    private static final int DEF_MARQUEE_TIME = 4;
    private static final int DEF_MARQUEE_STYLE = 3;
    private static final String DEF_MARQUEEN_STYLE_TWO_SPACE = "      ";//仅用在模式2下文本间距离像素

    private String originalText = "";

    private int mViewWidth = -1;
    private int spaceWidth;
    private int positionX;//模式2分界线
    private int destinationX;//模式2跳跃目标位置
    private int delayTime;

    private int marqueeStyle;//跑马灯风格
    private int borderWidth;//描边宽度
    private int marqueeTime;//跑马灯时间 单位/s
    private int currentScrollX;//跑马灯当前的位置
    private int textWidth;
    private int borderColor;
    private int currentStyle;

    private TextView borderText = null;///用于描边的TextView

    public StrokeMarqueeTextView(Context context) {
        super(context);
        borderText = new TextView(context);
        init(context,null);
    }

    public StrokeMarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        borderText = new TextView(context,attrs);
        init(context, attrs);
    }

    public StrokeMarqueeTextView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        borderText = new TextView(context,attrs,defStyle);
        init(context, attrs);
    }

    public void setBorderColor(int borderColor) {   //设置文字描边颜色
        this.borderColor = borderColor;
        borderText.setTextColor(borderColor);//设置描边颜色
    }

    public void setMarqueeStyle(int marqueeStyle) {   //设置跑马灯风格（1,2）
        this.marqueeStyle = marqueeStyle;
    }

    public void setBorderWidth(int borderWidth) {    //设置描边宽度
        this.borderWidth = borderWidth;
        TextPaint tp1 = borderText.getPaint();
        tp1.setStrokeWidth(borderWidth);     //设置描边宽度
        tp1.setStyle(Paint.Style.STROKE);    //对文字只描边
    }

    public void setMarqueeTime(int marqueeTime) {   //设置跑马总时间 单位/s
        this.marqueeTime = marqueeTime;
    }

    private void initLogicData() {
        pause = true;
        isCheckText = false;
        currentStyle = 3;
        changedFromStyleTwo = false;
    }

    private void initData() {
        isShortMarquee = false;
        isGetTextWidth = false;
        isFirstTime = true;
    }

    public void init(Context context, AttributeSet attrs){
//        Log.e("Constructor-->","");
        initLogicData();
        initData();
        if(null != attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StrokeMarqueeTextView);
            //获取控件属性值(跑马风格，时间，描边颜色,描边宽度)进行设置
            setBorderColor(a.getColor(R.styleable.StrokeMarqueeTextView_borderColor, DEF_BORDER_COLOR));
            setBorderWidth(a.getInteger(R.styleable.StrokeMarqueeTextView_borderWidth,DEF_BORDER_WIDTH));
            setMarqueeTime(a.getInteger(R.styleable.StrokeMarqueeTextView_marqueeTime,DEF_MARQUEE_TIME));
            setMarqueeStyle(a.getInteger(R.styleable.StrokeMarqueeTextView_marqueeStyle, DEF_MARQUEE_STYLE));

            a.recycle();
        }
        borderText.setGravity(this.getGravity());//设置底部文字位置
    }

    @Override
    public void setLayoutParams (ViewGroup.LayoutParams params){
        super.setLayoutParams(params);
        borderText.setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(true == isCheckText) {
            if (false == isGetTextWidth) {        //（测试阶段）
                getTextWidth();// 文字宽度只需获取一次
                isGetTextWidth = true;
            }

            CharSequence tt = borderText.getText();
            //两个TextView上的文字必须一致
            if(tt== null || !tt.equals(this.getText())){
                borderText.setText(getText());
                this.postInvalidate();
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        borderText.measure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onLayout (boolean changed, int left, int top, int right, int bottom){
        super.onLayout(changed, left, top, right, bottom);
        borderText.layout(left, top,textWidth, bottom);//BorderTextView右坐标设置成文本宽度值
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        initData();
        if(true == isCheckText && marqueeStyle == STYLE_TWO && false == isShortMarquee) {  //扩展文本为两倍长度 + 间隔符
//            Log.e("isShortMarquee","=false");
            StringBuffer sb = new StringBuffer(text);
            sb.append(DEF_MARQUEEN_STYLE_TWO_SPACE + text.toString());
            text = sb.toString();
        }
        super.setText(text, type);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(false == isCheckText) {
            mViewWidth = this.getWidth();//只需获取一次View的宽度
        }
        if (true == isCheckText && true == isFirstTime) {
            isShortMarquee = (textWidth <= mViewWidth)?true:false;//判断文本是否超出长度决定滚动否
            getDelayTime();//计算时间只需一次
            currentStyle = marqueeStyle;
            switch (marqueeStyle) {
                case STYLE_ONE:
                    scrollTo(-mViewWidth, 0);//设置初始位置
                    currentScrollX = -mViewWidth;//设置当前位置值
                    break;
                case STYLE_TWO:
                    scrollTo(0, 0);
                    currentScrollX = 0;
                    if(true == isShortMarquee) {  //如果短文本改为静态显示
                        currentStyle = 3;
                    }
                    break;
                case STYLE_THREE:             //文本未超出TextView不跑
                    scrollTo(0, 0);
                    currentScrollX = 0;
                    break;
            }
            isFirstTime = false;
        }
        borderText.draw(canvas);
        super.onDraw(canvas);

        if(false == isCheckText) { //进行文本初始化操作（与mViewWidth比较）
            isCheckText = true;
            Paint paint = this.getPaint();
            int textWidth = (int) paint.measureText(originalText);
            if(textWidth <= mViewWidth) {
                isShortMarquee = true;  //首次进行长短文本匹配
//                Log.e("onDraw匹配","首次进行长短文本匹配");
            }
            this.setText(originalText);
        }
//        Log.e("onDraw","ViewWidth=" + mViewWidth);
//        Log.e("onDraw","originalText=" + originalText);
    }

    @Override
    public void run() {
        if(true == pause) {
            pause = false;
            return;
        }
        currentScrollX += DEF_SCROLL_X;
        scrollTo(currentScrollX, 0);
        /**
         * 在2模式下移动到分界线后重绘文本位置
         */
        if(STYLE_TWO == currentStyle) {
            if(getScrollX() >= positionX) {
//                Log.e("run--> ","在2模式下移动到分界线后重绘文本位置");
//                Log.e("getScrollX()--> ","" + getScrollX());
                currentScrollX = destinationX ;
                scrollTo(currentScrollX, 0);
            }
        }else {
            if (getScrollX() >= textWidth) {//一次循坏结束
                boolean keepMarquee = false;//根据marqueeStyle判断是否循环跑马
                switch (marqueeStyle) {
                    case STYLE_ONE:
                        scrollTo(-mViewWidth, 0);
                        currentScrollX = -mViewWidth;
                        keepMarquee = true;
                        break;
                    case STYLE_TWO:
                        scrollTo(0, 0);
                        currentScrollX = 0;
                        keepMarquee = true;
                        break;
                    case STYLE_THREE:
                        scrollTo(0, 0);
                        currentScrollX = 0;
                        break;
                    default:
                }
                if(true != keepMarquee){
                    return;
                }
            }
        }
        postDelayed(this, delayTime);
    }
    // 开始滚动
    public void startScroll() {
        pause = false;
        if( (currentStyle == STYLE_ONE) || (currentStyle == STYLE_TWO && false == isShortMarquee) ) {
            this.removeCallbacks(this);
            post(this);
        }
    }

    private void getTextWidth() {
        Paint paint = this.getPaint();
        String text = this.getText().toString();
        textWidth = (int) paint.measureText(text);
        spaceWidth = (int) paint.measureText(DEF_MARQUEEN_STYLE_TWO_SPACE);
    }

    private void getDelayTime() {
        // 每步时间 = 总时间 / 总步数, 总步数 = 总长度 / 单步步长
        double time = DEF_MARQUEE_TIME;
        switch (marqueeStyle) {
            case STYLE_ONE:
                time = (double) (marqueeTime*1000) / ( ((double)textWidth + (double)mViewWidth) / (double)DEF_SCROLL_X);
                break;
            case STYLE_TWO:
                time = (double) (marqueeTime*1000) / ( ((double)((textWidth - spaceWidth)/2)) / (double)DEF_SCROLL_X);
                break;
            case STYLE_THREE:
                break;
        }
        delayTime =(int)Math.round(time);
        getDataInStyleTwo(isShortMarquee);
//        Log.e("文本宽度:","" + textWidth);
//        Log.e("TextView宽度:","" + mViewWidth);
//        Log.e("总步数:","" + ((textWidth + mViewWidth) / DEF_SCROLL_X));
//        Log.e("每步路时间:","" + delayTime);
//        Log.e("delayTime:", "" + time);
    }

    private void getDataInStyleTwo(boolean isShortMarquee){
        if(false == isShortMarquee) {//获取模式2下的位置数据
            positionX = textWidth + spaceWidth - mViewWidth;
            destinationX = textWidth/2 + spaceWidth/2  - mViewWidth;
        }
    }

    public void setMarqueeText(CharSequence text) {

        pause = true;//暂停滚动，重设文本
        originalText = text.toString();//保存原始文本

        if(true == changedFromStyleTwo) {
            marqueeStyle = STYLE_TWO;
            changedFromStyleTwo = false;
        }
        switch (marqueeStyle) {
            case STYLE_ONE:
                this.setText(originalText);
                break;
            case STYLE_TWO:
                if(mViewWidth > 0) {
                    Paint paint = this.getPaint();
                    int textWidth = (int) paint.measureText(originalText);
                    if(textWidth <= mViewWidth) {
                        changedFromStyleTwo = true;
                        isShortMarquee = true;
                        marqueeStyle = 3;
                    }else {
                        isShortMarquee = false;
                        marqueeStyle = STYLE_TWO;
                    }
                    this.setText(originalText);
                }
                break;
        }
    }
}