package com.android.bsl;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

public class LogoActivity extends Activity{

	
	private ImageView mImgLogo;
	
	private boolean mIsActionFinish = false;
	private boolean mIsInitFinish = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logo_layout);
		
//		if(mApp.mSharedPreferences.getBoolean("initOk", false)){
//			mIsInitFinish = true;
//		}
//		else{
//		}
		//TextView tv = (TextView)findViewById(R.id.textView);
		//tv.setSelected(true);

		mImgLogo = (ImageView)findViewById(R.id.imgLogo);
		mImgLogo.setImageResource(R.drawable.login);
		mHandler.sendEmptyMessageDelayed(0, 3000);
		//mHandler.sendEmptyMessage(1);
	}

	Handler mHandler = new Handler(){
    
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			
			switch(msg.what){
			case 0:
				startActivity(new Intent(LogoActivity.this,LoginActivity.class));
				finish();
				break;  
				
			}  
			super.handleMessage(msg);
		}
		
	};
	
}
