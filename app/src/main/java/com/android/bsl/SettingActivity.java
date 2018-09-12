package com.android.bsl;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class SettingActivity extends Activity implements OnClickListener{
	/** Called when the activity is first created. */
	/**
	 * 返回按钮
	 */
	private ImageView mBtnBack = null;

	/**
	 * 最右边的按钮
	 */
	private Button mBtnRightText = null;
	private ImageButton mBtnRightImg = null;
	
	/**
	 * 标题
	 */
	private TextView mTvTitle = null;
	/**
	 * 右边按钮分割线
	 */
	private ImageView mImgRight = null;
	/**
	 * 左边按钮分割线
	 */
	private ImageView mImgLeft = null;
	private ImageView mImgicon = null;
	
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
    }

	private void init() {
		// TODO Auto-generated method stub
		mBtnBack = (ImageView)findViewById(R.id.imgActionBarBack);
		mBtnRightImg = (ImageButton)findViewById(R.id.btnActionBarRightImg);
		mBtnRightText = (Button)findViewById(R.id.btnActionBarRightText);
		mTvTitle = (TextView)findViewById(R.id.tvActionBarTitle);
		mImgRight = (ImageView)findViewById(R.id.imgActionBarRight);
		mImgLeft = (ImageView)findViewById(R.id.imgActionBarLeft);
		mImgicon = (ImageView)findViewById(R.id.titleImgSiftIcon);
		
		
		mBtnBack.setOnClickListener(this);
		mBtnRightImg.setOnClickListener(this);
		mBtnRightText.setOnClickListener(this);
		mTvTitle.setOnClickListener(this);
		
		setActionBarView(true, false, "设置");
		
	}

	/**
	 * 
	 * @param backBtn 返回按钮是否可见
	 * @param rightTextBtn 右边文本按钮时否可见
	 * @param strTitle 设置中间的文本信息
	 * @param btnText 设置右边文本信息
	 */
	public void setActionBarView(boolean backBtn,boolean rightTextBtn,String strTitle){
		
		mTvTitle.setText(strTitle);
		//mBtnRightText.setText(btnText);
		
		if(rightTextBtn){
			mBtnRightImg.setVisibility(View.VISIBLE);
		}
		else{
			mBtnRightImg.setVisibility(View.INVISIBLE);
		}
		
		if(backBtn){
			mBtnBack.setVisibility(View.VISIBLE);
			mImgLeft.setVisibility(View.VISIBLE);
		}
		else{
			mBtnBack.setVisibility(View.INVISIBLE);
			mImgLeft.setVisibility(View.INVISIBLE);
		}
		
		if(rightTextBtn){
			mBtnRightText.setVisibility(View.VISIBLE);
			mImgRight.setVisibility(View.VISIBLE);
		}
		else{
			mBtnRightText.setVisibility(View.INVISIBLE);
			mImgRight.setVisibility(View.INVISIBLE);
		}
		
		if(false){
			mImgicon.setVisibility(View.INVISIBLE);
		}else{
			mImgicon.setVisibility(View.INVISIBLE);
		}
		
	}
	
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.btnActionBarRightImg:
		case R.id.btnActionBarRightText:
			
			break;
		case R.id.imgActionBarBack:
			finish();
			break;
		} 
	}
}
