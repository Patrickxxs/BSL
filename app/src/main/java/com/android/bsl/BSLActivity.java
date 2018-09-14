package com.android.bsl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BSLActivity extends Activity implements OnClickListener {
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
	private ListView listview;
	private GalleryFlow galleryflow;
	private List<NodeInfo> nodelist;
	private MyAdapter myAdapter;
	public  ClientThread rxListenerThread;
	public static Handler mainHandler;
	/*private String[] scensString=new String[]
			{
			"空调","灯光","屏幕"};"次卧","书房","餐厅","浴室","阳台",
			"花园"
			};*/
	private TextView sceneView;
	public static boolean isConnected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init();
		
	}

	private void init() {
		// TODO Auto-generated method stub
		mBtnBack = (ImageView) findViewById(R.id.imgActionBarBack);
		mBtnRightImg = (ImageButton) findViewById(R.id.btnActionBarRightImg);
		mBtnRightText = (Button) findViewById(R.id.btnActionBarRightText);
		mTvTitle = (TextView) findViewById(R.id.tvActionBarTitle);
		mImgRight = (ImageView) findViewById(R.id.imgActionBarRight);
		mImgLeft = (ImageView) findViewById(R.id.imgActionBarLeft);
		mImgicon = (ImageView) findViewById(R.id.titleImgSiftIcon);
		sceneView=(TextView)findViewById(R.id.sceneName);
		listview=(ListView)findViewById(R.id.mylist);
		myAdapter=new MyAdapter();
		listview.setAdapter(myAdapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				NodeInfo nodeinfo=nodelist.get(arg2);
				
				if(nodeinfo.getType()==0x15||nodeinfo.getType()==0x0B||nodeinfo.getType()==0x13||nodeinfo.getType()==0x14)
				{

					Intent intent=new Intent();
					if(nodeinfo.getType()==0x15)
					{
						ControlInterface.currentUiName="红外测距传感器";
					}
					else if(nodeinfo.getType()==0x14)
					{
						ControlInterface.currentUiName="声音传感器";
					}
					else if(nodeinfo.getType()==0x0B)
					{
						ControlInterface.currentUiName="温湿度传感器";
						
					}
					else if(nodeinfo.getType()==0x13)
					{
						ControlInterface.currentUiName="光照传感器";
					}
					
					intent.setClass(BSLActivity.this, ControlInterface.class);
					Bundle bundle=new Bundle();
					bundle.putInt("type", nodeinfo.getType());
					bundle.putParcelable("node", nodeinfo);
					intent.putExtras(bundle);
					startActivity(intent);
				}

					
				
			}
			
		});
		rxListenerThread = new ClientThread(this);//建立客户端线程
		nodelist=rxListenerThread.nodelist;
		
		rxListenerThread.start();
		
		initMainHandler();
		

		mBtnBack.setOnClickListener(this);
		mBtnRightImg.setOnClickListener(this);
		mBtnRightText.setOnClickListener(this);
		mTvTitle.setOnClickListener(this);

		setActionBarView(false, true, "主界面");
		/*Integer[] images = { R.drawable.room_gallery_0, R.drawable.room_gallery_1,
				R.drawable.room_gallery_2}; R.drawable.room_gallery_3, R.drawable.room_gallery_5,
				R.drawable.room_gallery_7,R.drawable.room_gallery_8,R.drawable.room_gallery_9,
				R.drawable.room_gallery_13};*/

		//ImageAdapter adapter = new ImageAdapter(this, images);
		//adapter.createReflectedImages();// 创建倒影效果
		GalleryFlow galleryFlow = (GalleryFlow) this.findViewById(R.id.gallery);
		galleryFlow.setFadingEdgeLength(0);
		galleryFlow.setSpacing(-30); // ͼƬ֮��ļ��
		//galleryFlow.setAdapter(adapter);
		galleryFlow.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {//跳

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				//sceneView.setText(scensString[arg2]);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		}
		);
	}
		
		/*galleryFlow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
				if(arg2==0)
				{
					//++++++++++++++++++++++++++
				//添加温湿度传感器
					NodeInfo nodeys0=new NodeInfo();
					nodeys0.setType((byte) 0x0b);
					nodeys0.setNodeName("温湿度传感器");
					nodeys0.setInfo("温度：25.56℃\n湿度：26.98%");
					nodelist.add(nodeys0);
					//添加光照传感器
					NodeInfo nodeys1=new NodeInfo();
					nodeys1.setType((byte) 0x13);
					nodeys1.setNodeName("光照传感器");
					nodeys1.setInfo("光强：270lx");
				nodelist.add(nodeys1);
					//添加人体红外传感器
					NodeInfo nodeys2=new NodeInfo();
					nodeys2.setType((byte) 0x15);
					nodeys2.setNodeName("红外测距传感器");
					nodeys2.setInfo("距离：57");
				nodelist.add(nodeys2);
					//++++++++++++++++++++++++++
				}
			}
		

	};*/

	/**
	 * 
	 * @param backBtn
	 *            返回按钮是否可见
	 * @param rightTextBtn
	 *            右边文本按钮时否可见
	 * @param strTitle
	 *            设置中间的文本信息
	 * @param btnText
	 *            设置右边文本信息
	 */
	public void setActionBarView(boolean backBtn, boolean rightTextBtn,
			String strTitle) {

		mTvTitle.setText(strTitle);
		// mBtnRightText.setText(btnText);

		if (rightTextBtn) {
			mBtnRightImg.setVisibility(View.VISIBLE);
		} else {
			mBtnRightImg.setVisibility(View.INVISIBLE);
		}

		if (backBtn) {
			mBtnBack.setVisibility(View.VISIBLE);
			mImgLeft.setVisibility(View.VISIBLE);
		} else {
			mBtnBack.setVisibility(View.INVISIBLE);
			mImgLeft.setVisibility(View.INVISIBLE);
		}

		if (rightTextBtn) {
			mBtnRightText.setVisibility(View.VISIBLE);
			mImgRight.setVisibility(View.VISIBLE);
		} else {
			mBtnRightText.setVisibility(View.INVISIBLE);
			mImgRight.setVisibility(View.INVISIBLE);
		}

		if (false) {
			mImgicon.setVisibility(View.INVISIBLE);
		} else {
			mImgicon.setVisibility(View.INVISIBLE);
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btnActionBarRightImg:
		case R.id.btnActionBarRightText:
			startActivity(new Intent(this, SettingActivity.class));
			break;
		case R.id.imgActionBarBack:

			break;
		}
	}

	void initMainHandler(){
		 
		 mainHandler = new Handler() {
			 
	            @Override
	            /**
	             * 主线程消息处理中心
	             */
	            public void handleMessage(Message msg) {
	                
	            	// 接收子线程的消息
	            	switch (msg.what) {
					case 0x1111:
						myAdapter.notifyDataSetChanged();
						break;
					case 0x1112:
						Bundle bundle=msg.getData();
						byte[]buffer=bundle.getByteArray("sendData");
						OutputStream outputStream=rxListenerThread.getOutPutStream();
						try {
							outputStream.write(buffer);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case 0x5555:
						isConnected=true;
						Toast.makeText(BSLActivity.this, "连接成功！",Toast.LENGTH_LONG).show();
						sendBroadcast(new Intent("com.android.action.nettestfinished"));
						break;
					case 0x5556:
						isConnected=false;
						Toast.makeText(BSLActivity.this, "连接失败！",Toast.LENGTH_LONG).show();
						sendBroadcast(new Intent("com.android.action.nettestfinished"));
						finish();
						break;
					default:
						break;
					}
	                
	            }
	 
	        };
		
		
	}
	
	
	
	
    private class MyAdapter extends BaseAdapter
    {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if(nodelist!=null)
			return nodelist.size();
			else return 0;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			
			ViewHolder viewholder;
			if(arg1==null)
			{
				arg1=getLayoutInflater().inflate(R.layout.listitem, null);
				viewholder=new ViewHolder();
				viewholder.imageview=(ImageView)arg1.findViewById(R.id.sensor_icon);
				viewholder.sensorNameView=(TextView)arg1.findViewById(R.id.sensor_name);
				viewholder.infoView=(TextView)arg1.findViewById(R.id.sensor_info);
				viewholder.addressView=(TextView)arg1.findViewById(R.id.sensor_address);
				arg1.setTag(viewholder);
			}
			else
			{
				viewholder=(ViewHolder)arg1.getTag();
			}
			viewholder.imageview.setImageResource(getIcon(nodelist.get(arg0).getType()));
			viewholder.sensorNameView.setText(nodelist.get(arg0).getNodeName()==null?"":nodelist.get(arg0).getNodeName());
			viewholder.infoView.setText(nodelist.get(arg0).getInfo()==null?"":nodelist.get(arg0).getInfo());
			viewholder.addressView.setText(nodelist.get(arg0).getId()==null?"":nodelist.get(arg0).getId());
			return arg1;
		}
    	
		private class ViewHolder
		{
			ImageView imageview;
			TextView sensorNameView;
			TextView infoView;
			TextView addressView;
		}
    }
    
    
    private int getIcon(int arg0)
    {
    	switch(arg0)
    	{
    	case 0x01:
    		
    		return R.drawable.wendu;
    	case 0x02:
    	case 0x03:	
    		return R.drawable.device_default_lightsensor;
    	case 0x04:
    		
    		return R.drawable.jiujing;
    	case 0x05:
    		return R.drawable.kongqi;
    	case 0x06:
    		return R.drawable.device_normal_smoke;
    	case 0x07:
    		return R.drawable.device_normal_pir;
    	case 0x08:
    		return R.drawable.device_shutter_mid;
    	case 0x09:
    		return R.drawable.device_temp;
    	case 0x0a:
    		return R.drawable.jiasudu;
    	case 0x0b:
    		return R.drawable.device_temp;
    	case 0x0c:
    		return R.drawable.tuoluo;
    	case 0x0d:
    		
    		return R.drawable.deng;
    	case 0x0e:
    		return R.drawable.zhinengdianbiao;
    	case 0x0f:
    		return R.drawable.yuanchengyaokong;
    		case 0x11:
    			return R.drawable.shake;
    	case 0x13:
    		return R.drawable.light;
    	case 0x14:
    		return R.drawable.voice;
    		case 0x15:
    		return R.drawable.range;
    	case 0x17:
    		return R.drawable.jidianqi;
    		
    				
    		
    		
    	}
    	return R.drawable.ic_launcher;
    }

	@Override
	public void onBackPressed() {
		
	//	super.onBackPressed();
		new AlertDialog.Builder(BSLActivity.this).setTitle("退出确定").setMessage("是否现在退出？").setPositiveButton("确定",new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				rxListenerThread.interrupt();
			
				Socket socket=rxListenerThread.getSocket();
				try {
					socket.close();
					//rxListenerThread.childHandler.sendEmptyMessage(0x01);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				dialog.dismiss();
				BSLActivity.this.finish();
				
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				
			}
		}).create().show();
		
	
	}
    
    
    
}