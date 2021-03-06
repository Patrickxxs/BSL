package com.android.bsl;

import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ControlInterface extends Activity {	
public static NodeInfo nodeinfo;
private int sensorType;
public static int temp_a=30;
//Float.parseFloat((bb.geText())
private boolean isbtn1Checked,isbtn2Checked,isbtn3Checked,isbtn4Checked;
private int currentProcess;
private TextView titleView;
private Timer timer;
public static Handler handler;
private View view1,view2,view3,view4;
RadioGroup mada_radioGroup;
RadioButton mada_rb1;
RadioButton mada_bb2;
SeekBar mada_seekBar;
Button mada_button;
private ImageView io_deng1;
private ImageView io_computer1;

private ToggleButton io_btn1;
private ToggleButton io_btn2;

private Button io_sureBtn;
private Button io_sure1Btn;
private Button io_sure2Btn;
private Button io_sure3Btn;

private TextView info;
private TextView info_1;
private  TextView soundInfo;
private TextView kongtiaostate;
private TextView jiashistate,distance;
private EditText et=null;
private Button open1;
private Button open2;
private Button close1;
private Button close2;


private static final String TAG="ControlInterface";
private boolean[]btnCheckedState=new boolean[4];
private boolean[]btnRecordState=new boolean[4];
private byte lastDengState=-1;
private boolean initFinished;
private byte mada_turn;
private int mada_sudu;
private byte dengState=0;
private byte computerState=0;
private byte KongtiaoState=0;
public static String currentUiName="";
private Timer timer2;
@Override
protected void onCreate(Bundle savedInstanceState) {
	// TODO Auto-generated method stub
	super.onCreate(savedInstanceState);
	Intent intent=getIntent();
	Bundle bundle=intent.getExtras();
	nodeinfo=(NodeInfo)bundle.getParcelable("node");
	sensorType=bundle.getInt("type");
	view1=getLayoutInflater().inflate(R.layout.distance_control, null);
	view2=getLayoutInflater().inflate(R.layout.deng_control, null);
	view3=getLayoutInflater().inflate(R.layout.temperature_control, null);
	view4=getLayoutInflater().inflate(R.layout.camera_control,null);
	
	switch(sensorType)
	{
		case 0x15:
			
			setContentView(view1);
			titleView=(TextView)findViewById(R.id.tvActionBarTitle);
			titleView.setText("红外测距传感器");
			ProcessView1();
			
			break;
		case 0x0B:
		
			setContentView(view3);
			titleView=(TextView)findViewById(R.id.tvActionBarTitle);
			titleView.setText("空调控制器");
			ProcessView3(nodeinfo);
			
			
			break;
		case 0x13:
			
			setContentView(view2);
			titleView=(TextView)findViewById(R.id.tvActionBarTitle);
			titleView.setText("灯光控制器");
			ProcessView2();
			
			
			break;
			
		case 0x14:
			
			setContentView(view4);
			titleView=(TextView)findViewById(R.id.tvActionBarTitle);
			titleView.setText("声音传感器");
			ProcessView4();
			
			
			break;
	}
	


	
	handler=new Handler()
	{

		@Override
		public void handleMessage(Message msg)  //信息更新
		{
	
			super.handleMessage(msg);
			switch(msg.what)
			{
			case 0x2222:

				nodeinfo=(NodeInfo)msg.obj;
				info_1.setText(nodeinfo.getInfo());
				if(io_sureBtn.getText()=="手动控制")
				{
				changeDengUIState();
				}
				else
				{
					
				}
			
				break;
			case 0x2223:
				nodeinfo=(NodeInfo)msg.obj;
				info.setText(nodeinfo.getInfo());
				if(io_sure2Btn.getText()=="手动控制")
				{
				changeKongtiaoUIState();
				}
				else
				{
					
				}
				
				break;
			case 0x2224:
				nodeinfo=(NodeInfo)msg.obj;
				distance.setText(nodeinfo.getInfo());
				if(io_sure1Btn.getText()=="手动控制")
				{
				changeComputerUIState();
				}
				else
				{
					
				}
				
				break;
			case 0x2225:
                    nodeinfo=(NodeInfo)msg.obj;
                    soundInfo.setText(nodeinfo.getInfo());
                    if(io_sure3Btn.getText()=="关闭安保模式")
                    {
                        changeCameraUIState();
                    }
                    else
                    {

                    }

                    break;

			default :
					
				break;
			}
		}
		
	};
	
}

private void ProcessView1()   //红外测距控制
{
	Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getComputerState());
	io_computer1=(ImageView)view1.findViewById(R.id.imageView1);
	io_btn2=(ToggleButton)view1.findViewById(R.id.toggleButton1);
	io_sure1Btn=(Button)view1.findViewById(R.id.sure1);
	
	distance=(TextView)view1.findViewById(R.id.distance);
	distance.setText(nodeinfo.getInfo());
	io_sure1Btn.setOnClickListener(new MyOnClickListener(){
		@Override
		public void onClick(View v)
		{
			if(io_sure1Btn.getText()=="自动控制")
			{
				io_sure1Btn.setText("手动控制");
				io_btn2.setVisibility(View.GONE);
				changeComputerUIState();
			}
			else
			{
				io_sure1Btn.setText("自动控制");
				io_btn2.setVisibility(View.VISIBLE);
			}
			
			
		}
	});
	io_btn2.setOnCheckedChangeListener(new OnCheckedChangeListener()
	{

		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean arg1) {  //电脑状态的改变
			btnCheckedState[0]=arg1;
			int st=io_computer1.getVisibility();
			computerState=nodeinfo.getComputerState();
			if(io_btn2.isChecked())
			{
				io_computer1.setImageResource(R.drawable.device_computer_open);
			//	io_deng1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_close));
				
			
			}
			else {
				io_computer1.setImageResource(R.drawable.device_computer_close);
		//		io_btn1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_open));
			
			}
			
		}
					
	});
	
	
}

private void ProcessView2()    //光照
{
	info_1=(TextView)view2.findViewById(R.id.info_1);
	info_1.setText(nodeinfo.getInfo());
	Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getDengState());
	io_deng1=(ImageView)view2.findViewById(R.id.light1);

	io_btn1=(ToggleButton)view2.findViewById(R.id.control_1);
	
	io_sureBtn=(Button)view2.findViewById(R.id.sure);

	io_sureBtn.setOnClickListener(new MyOnClickListener(){
		@Override
		public void onClick(View v)
		{
			if(io_sureBtn.getText()=="自动控制")
			{
				io_sureBtn.setText("手动控制");
				io_btn1.setVisibility(View.GONE);
				changeDengUIState();
			}
			else
			{
				io_sureBtn.setText("自动控制");
				io_btn1.setVisibility(View.VISIBLE);
			}
			
			
		}
	});

	io_btn1.setOnCheckedChangeListener(new OnCheckedChangeListener()
	{

		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
			btnCheckedState[0]=arg1;
			int st=io_deng1.getVisibility();
			dengState=nodeinfo.getDengState();
			if(io_btn1.isChecked())
			{
				io_deng1.setImageResource(R.drawable.device_wallbtn_open);
			//	io_deng1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_close));
				
			
			}
			else {
				io_deng1.setImageResource(R.drawable.device_wallbtn_close);
		//		io_btn1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_open));
			
			}
//			if(st==View.VISIBLE)
//			{
//				io_deng1.setVisibility(View.GONE);
//			}
//			else
//			{
//				io_deng1.setVisibility(View.VISIBLE);
//			}
				
			
		}
		
	});
//changeDengUIState();


}

private void ProcessView3(NodeInfo nodeinfo)
{
	info=(TextView)view3.findViewById(R.id.info);
	info.setText(nodeinfo.getInfo());
	kongtiaostate=(TextView)view3.findViewById(R.id.kongtiaostate);
	io_sure2Btn=(Button)view3.findViewById(R.id.sure_2);
	//et = (EditText)findViewById(R.id.et);
	kongtiaostate.setText("空调状态：关闭");
	//jiashistate=(TextView)view3.findViewById(R.id.jiashistate);
	//jiashistate.setText("加湿器状态：关闭");
	
	open1=(Button)view3.findViewById(R.id.open1);
	io_sure2Btn.setOnClickListener(new MyOnClickListener(){//314-332
		@Override
		public void onClick(View v)
		{
			if(io_sure2Btn.getText()=="自动控制")
			{
				io_sure2Btn.setText("手动控制");
				open1.setVisibility(View.GONE);
				//open2.setVisibility(View.GONE);
				close1.setVisibility(View.GONE);
				//close2.setVisibility(View.GONE);
				changeKongtiaoUIState();
			}
			else
			{
				io_sure2Btn.setText("自动控制");
				open1.setVisibility(View.VISIBLE);
				//open2.setVisibility(View.VISIBLE);
				close1.setVisibility(View.VISIBLE);
				//close2.setVisibility(View.VISIBLE);
			}
			
			
		}
	});

	open1.setOnClickListener(new View.OnClickListener()
	{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			kongtiaostate.setText("空调状态：打开");
		}
		
	});
	close1=(Button)view3.findViewById(R.id.close1);

	close1.setOnClickListener(new View.OnClickListener()
	{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			kongtiaostate.setText("空调状态：关闭");
		}
		
	});
	//open2=(Button)view3.findViewById(R.id.open2);

	/*open2.setOnClickListener(new View.OnClickListener()
	{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			jiashistate.setText("加湿器状态：打开");
		}
		
	});*/
	//close2=(Button)view3.findViewById(R.id.close2);

	/*close2.setOnClickListener(new View.OnClickListener()
	{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			jiashistate.setText("加湿器状态：关闭");
		}
		
	});*/
}

	private void ProcessView4()    //声音
	{
		soundInfo=(TextView)view4.findViewById(R.id.soundInfo);
		soundInfo.setText(nodeinfo.getInfo());
		//Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getDengState());
		io_deng1=(ImageView)view4.findViewById(R.id.camera);



		io_sure3Btn=(Button)view4.findViewById(R.id.securitymode);

		io_sure3Btn.setOnClickListener(new MyOnClickListener(){
			@Override
			public void onClick(View v)
			{
				if(io_sure3Btn.getText()=="安保模式")
				{
					io_sure3Btn.setText("关闭安保模式");

					changeCameraUIState();
				}
				else
				{
					io_sure3Btn.setText("安保模式");

				}


			}
		});




	}


private  void changeCameraUIState()
{
    Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getDengState());

   //
    if(nodeinfo.getInfo() == "检测到声音")
    {
        io_deng1.setImageResource(R.drawable.device_wallbtn_close);
       // io_btn1.setChecked(false);
    }
    else {
        io_deng1.setImageResource(R.drawable.device_wallbtn_open);
       // io_btn1.setChecked(true);
    }

}
private void changeDengUIState()
{
	Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getDengState());
//	if(nodeinfo.getDengState()!=dengState)
//	{
	
		dengState=nodeinfo.getDengState();
		if(dengState!=0)
		{
		//	io_deng1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_close));
			io_deng1.setImageResource(R.drawable.device_wallbtn_close);
			io_btn1.setChecked(false);
		}
		else {
			io_deng1.setImageResource(R.drawable.device_wallbtn_open);
	//		io_btn1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_open));
			io_btn1.setChecked(true);
		}
	
	//}
	}

private void changeComputerUIState()
{
	Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getComputerState());
//	if(nodeinfo.getDengState()!=dengState)
//	{
	
		computerState=nodeinfo.getComputerState();
		if(computerState!=0)
		{
		//	io_deng1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_close));
			io_computer1.setImageResource(R.drawable.device_computer_close);
			io_btn2.setChecked(false);
		}
		else {
			io_computer1.setImageResource(R.drawable.device_computer_open);
	//		io_btn1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_open));
			io_btn2.setChecked(true);
		}
	
	//}
	}
private void changeKongtiaoUIState(){
	Log.i(TAG,"YYYYYYYYYYYYYYYY"+nodeinfo.getKongtiaoState());
//	if(nodeinfo.getDengState()!=dengState)
//	{
	
	KongtiaoState=nodeinfo.getKongtiaoState();
		if(KongtiaoState!=0)
		{
		//	io_deng1.setBackgroundDrawable(getResources().getDrawable(R.drawable.device_wallbtn_close));
			//io_kongtiao1.setImageResource(R.drawable.device_computer_close);
			kongtiaostate.setText("空调状态：开启");
			//io_btn3.setChecked(false);
		}
		else {
			kongtiaostate.setText("空调状态：关闭");
		}
	
	//}
}
private class MyOnClickListener implements View.OnClickListener
{

	@Override
	public void onClick(View v)
	{
		clickProcess();
		
	}
	
}


public void clickProcess()
{
	isbtn1Checked=io_btn1.isChecked();

	
	byte[]buffer=new byte[18];
	buffer[0]=buffer[1]=(byte)0xff;
	buffer[2]=0x12;
	buffer[3]=(byte)13;
	String temp1=(nodeinfo.getId().substring(0, 2));
	String temp2=(nodeinfo.getId().substring(2, 4));
	System.out.println(temp1);
	System.out.println(temp2);
	buffer[4]=(byte)(Integer.parseInt(temp1,16));
	buffer[5]=(byte)(Integer.parseInt(temp2,16));
	buffer[6]=(byte)0x08;
	if(isbtn1Checked)
	buffer[7]=(byte)0x01;
	else {
		buffer[7]=(byte)0x00;
	}

	
	for(int i=8;i<17;i++)
	{
	buffer[i]=(byte)0xfe;
	}
	byte[]temp=new byte[17];
	for(int i=0;i<temp.length;i++)
	{
		temp[i]=buffer[i];
	}
	byte temp3=(byte)0x00;
	
	for(int i=0;i<temp.length;i++)
	{
		temp3^=temp[i];
	}
	buffer[17]=temp3;
	Message message=new Message();
	Bundle bundle=new Bundle();
	bundle.putByteArray("sendData", buffer);
	message.what=0x1112;
	message.setData(bundle);
	Log.i(TAG,"ONCLICKED");
	BSLActivity.mainHandler.sendMessage(message);
}

@Override
protected void onPause()
{
	// TODO Auto-generated method stub
	super.onPause();

}


/*
private byte decodeRecordeState()
{
	int temp=0;
	if(btnRecordState[0])
		temp+=1000;
	if(btnRecordState[1])
		temp+=100;
	if(btnRecordState[2])
		temp+=10;
	if(btnRecordState[3])
		temp+=1;
	byte result=0;
	switch(temp)
	{
	case 1111:
		result=(byte)0x00;
		break;
	case 1110:
		result=(byte)0x10;
		break;
	case 1101:
		result=(byte)0x20;
		break;
	case 1100:
		result=(byte)0x30;
		break;
	case 1011:
		result=(byte)0x40;
		break;
	case 1010:
		result=(byte)0x50;
		break;
	case 1001:
		result=(byte)0x60;
		break;
	case 1000:
		result=(byte)0x70;
		break;
	case 111:
		result=(byte)0x80;
		break;
	case 110:
		result=(byte)0x90;
		break;
	case 101:
		result=(byte)0xa0;
		break;
	case 100:
		result=(byte)0xb0;
		break;
	case 11:
		result=(byte)0xc0;
		break;
	case 10:
		result=(byte)0xd0;
		break;
	case 1:
		result=(byte)0xe0;
		break;
	case 0:
		result=(byte)0xf0;
		break;
	}
	return result;
}

private void syncRecord()
{
	for(int i=0;i<4;i++)
	{
		btnRecordState[i]=btnCheckedState[i];
	}
}
*/
private void changeMaDaUi()
{
	if(initFinished&&mada_turn==nodeinfo.getMada_turnto()&&mada_sudu==nodeinfo.getMada_sudu())
	{
		return ;
	}
	if(nodeinfo.getMada_turnto()==1)
	{
		mada_rb1.setChecked(true);
	}
	else
	{
		mada_bb2.setChecked(true);
	}
	mada_seekBar.setProgress(nodeinfo.getMada_sudu());
	mada_turn=nodeinfo.getMada_turnto();
	mada_sudu=nodeinfo.getMada_sudu();
	initFinished=true;
}
}
