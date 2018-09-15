package com.android.bsl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

@SuppressLint("HandlerLeak")
public class ClientThread extends Thread {
	private OutputStream outputStream = null;
	private InputStream inputStream = null;
	private Socket socket;
	private SocketAddress socketAddress;
	public static Handler childHandler;
	private boolean key = true;

	private RxThread rxThread;
	public static final int DataCacheSize = 1024;
	static int DataCache_Start = 0, DataCache_End = 0;
	static byte[] DataCache = new byte[DataCacheSize];

	DecimalFormat floatNum = new DecimalFormat("###0.00");
	// 节点信息更新
	private String StrC_NA = "";
	private String StrC_IEEE = "";
	private String StrC_VER = "";
	private String StrC_PANID = "";
	private String StrP_NA = "";
	private String StrP_IEEE = "";
	private String StrPOWER_VOL = "";
	private String StrRUN_TIME = "";
	private String StrSENSOR_TYPE = "";
	private String StrSENSOR_DATA = "";
	private String StrNODE_GATHERTIME = "";
	private String StrNODE_BAUD = "";
	private String StrPROFILE = "";
	private Integer FrameErrorNum = 0;
	private Integer FrameNum = 0;
	public static List<NodeInfo> nodelist = new ArrayList<NodeInfo>();
	private Context context;
	private SharedPreferences sharedPreferences;
	private String ip;
	private String port;

	public ClientThread(Context context) {

		this.context = context;
		sharedPreferences = context.getSharedPreferences("ip&port",
				Context.MODE_PRIVATE);
		ip = sharedPreferences.getString("ip", "192.168.100.10");
		port = sharedPreferences.getString("port", "8000");

	}

	/**
	 * 连接
	 */
	void connect() {
		key = true;
		try {
			socketAddress = new InetSocketAddress(ip, Integer.parseInt(port));
			socket = new Socket();
			socket.connect(socketAddress, 10000);
			inputStream = socket.getInputStream();//数据流输入输出，input从缓存中读，output进入缓存
			outputStream = socket.getOutputStream();
			//发送数据
			//String str ="6";
			//outputStream.write(str.getBytes());
			
			if (socket.isConnected()) {
				rxThread = new RxThread();
				rxThread.start();
				BSLActivity.mainHandler.sendEmptyMessage(0X5555);
			} else {
				BSLActivity.mainHandler.sendEmptyMessage(0X5556);
			}

		} catch (ConnectException e) {
			BSLActivity.mainHandler.sendEmptyMessage(0X5556);
		} catch (SocketException e) {
			BSLActivity.mainHandler.sendEmptyMessage(0X5556);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			// Log.d("Error", "与服务端连接失败...");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block

		}

	}

	void initChildHandler() {

		// 在子线程中创建Handler必须初始化Looper
		Looper.prepare();

		childHandler = new Handler() {
			/**
			 * 子线程消息处理中心
			 */
			public void handleMessage(Message msg) {

				// 接收主线程及其他线程的消息并处理...
				switch (msg.what) {
				case 0:

					try {
						outputStream.write(((String) (msg.obj)).getBytes());
						outputStream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;

				case 1:

					key = false;
					try {
						if (inputStream != null)
							inputStream.close();
						if (outputStream != null)
							outputStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					childHandler.getLooper().quit();// 结束消息队列

					break;

				default:
					break;
				}

			}
		};

		// 启动该线程的消息队列
		Looper.loop();

	}

	public void run() {
		connect();
		initChildHandler();

	}

	public class RxThread extends Thread {

		public void run() {

			// printClass.printf("���������߳�");
			byte[] buffer = new byte[1024];

			while (key) {

				try {
					int readSize = inputStream.read(buffer);
					if (readSize > 0) {
						// String str = new String(buffer, 0, readSize);

						String str = DecodeData2Str(buffer, readSize);
						BSLActivity.mainHandler.sendEmptyMessage(0x1111);
						// Log.d("Message:", str);
						// printClass.printf("<< " + str);

					} else {

						inputStream.close();
						Log.d("error:", "close connect...");
						// printClass.printf("与服务器断开连接");
						break;

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				if (socket.isConnected())
					socket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	// 将数据包译码成相应的数据
	public String DecodeData2Str(final byte[] SrcData, int size) {
		int i = 0, j = 0;
		int FreeCache = 0; // 缓冲区剩余空间
		byte[] DecPacket = new byte[256]; // 缓存---译码出来的数据包(最大数据包限制)
		int DecPacketLen = 0; // 译码出的数据包长度
		byte FrameDataLen = 0; // 数据部分译码后的理论长度
		int FrameStart = -1; // 帧起始位置
		boolean IsFoundFullFrame = false; // 是否找到一个完整的数据帧(false:否, true:是)
		boolean IsValidFrame = false; // 数据帧CRC校验是否通过(false:否, true:是)
		byte CheckCRC = (byte) 0x00; // 待检验参考CRC
		byte CalCRC = (byte) 0x00; // 计算CRC
		byte FrameType = 0; // 帧类型
		Float Volatage = 0.0f;
		StringBuilder stringBuilder = new StringBuilder(""); // 解码出的信息

		FreeCache = DataCacheSize - DataCache_End;
		if (FreeCache < size) {
			DataCache_Start = 0;
			DataCache_End = 0;
		}

		for (i = 0; i < size; i++) {
			DataCache[DataCache_End++] = SrcData[i];
		}

		if (DataCache_End - DataCache_Start >= 9) {
			for (i = DataCache_Start; i < (DataCache_End - 2); i++) {
				IsFoundFullFrame = false; // 清除完整数据帧标志位
				IsValidFrame = false; // 清除CRC校验标志位
				CalCRC = (byte) 0x00; // 清除根据数据帧计算得出的CRC
				FrameStart = -1; // 清除帧起始位置

				// 查找起始帧标志��־
				if (IsFrameFront(DataCache, i)) {
					FrameStart = i;
					DecPacketLen = 2;
					byte DecDataLen = 0;
					DecPacket[DecPacketLen++] = DataCache[FrameStart + 2];
					FrameType = DataCache[FrameStart + 2];
					DecPacket[DecPacketLen++] = DataCache[FrameStart + 3];
					FrameDataLen = DataCache[FrameStart + 3];
					if (FrameDataLen + FrameStart < DataCache_End) {
						IsFoundFullFrame = true;
						for (j = FrameStart + 4; j < (DataCache_End - 1); j++) {
							if (DecDataLen == FrameDataLen) {
								break;
							}
							// System.out.println("=======================>"+DataCache_End);
							DecPacket[DecPacketLen++] = DataCache[j];
							DecDataLen++;
						}
						CheckCRC = DataCache[j];

						// CRC校验
						for (j = 0; j < DecPacketLen; j++) {
							CalCRC = (byte) (CalCRC ^ DecPacket[j]);
						}
						if (CalCRC == CheckCRC) {
							IsValidFrame = true;
							DataCache_Start = DataCache_Start + DecDataLen + 5;
						} else if (IsFoundFullFrame) {
							DataCache_Start = DataCache_Start + 2;
						}

						// 完整的数据帧֡
						if (IsFoundFullFrame) {
							if (!IsValidFrame) {
								stringBuilder
										.append("------------------------------------------------------------\n");
								stringBuilder.append("数据帧CRC校验失败：\n");
								stringBuilder.append("接收CRC：0x"
										+ Byte2HexStr(CheckCRC) + "   ");
								stringBuilder.append("计算CRC：0x"
										+ Byte2HexStr(CalCRC) + "\n");
								stringBuilder.append("数据帧：\n");
								for (j = 0; j < DecPacketLen; j++) {
									stringBuilder
											.append(Byte2HexStr(DecPacket[j]));
								}
								stringBuilder.append("\n");
							} else {
								// 对译码后的数据包进行解析

								byte[] temp = new byte[1024];
								int count = 0;
								for (int kk = DataCacheSize + FrameStart; kk < DataCache_End; kk++) {
									temp[count++] = DataCache[kk];
								}
								DataCache = temp;

								DataCache_End = DataCache.length;
								DataCache_Start = 0;

								switch (FrameType) {

								case (byte) 0x01: // 发送节点地址，父节点地址，协议版本

									stringBuilder
											.append("\n------------------------------------------------------------\n");
									stringBuilder.append("节点信息：\n");
									stringBuilder.append("节点地址：0x");

									for (j = 0; j < 2; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[4 + j]));

									}

									StrC_NA = Byte2HexStr(DecPacket[4])
											+ Byte2HexStr(DecPacket[5]);
									// nodeinfo.setId(Integer.toHexString(Integer.parseInt(StrC_NA,
									// 16)));
									// nodeinfo.setId(StrC_NA);
									stringBuilder.append("\n" + "父节点地址：0x");
									for (j = 0; j < 2; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[6 + j]));
									}

									StrP_NA = Byte2HexStr(DecPacket[6])
											+ Byte2HexStr(DecPacket[7]);
									// nodeinfo.setParentId(StrP_NA);
									stringBuilder.append("\n" + "协议栈版本:");
									if (Byte2HexStr(DecPacket[8]).equals("10")) {
										stringBuilder.append("ZigBee 2007");
										StrPROFILE = "ZigBee 2007";
									} else if (Byte2HexStr(DecPacket[8])
											.equals("11")) {
										stringBuilder.append("ZigBee 2007 pro");
										StrPROFILE = "ZigBee 2007 pro";
									} else {
										stringBuilder
												.append("##Error##协议栈版本错误，请校验");
									}
									break;

								case (byte) 0x02: // 节点综合信息报告
									NodeInfo nodeinfo = new NodeInfo();
									stringBuilder
											.append("\n------------------------------------------------------------\n");
									stringBuilder.append("节点综合信息报告：\n");
									stringBuilder.append("节点地址:0x");
									for (j = 0; j < 2; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[4 + j]));
									}
									StrC_NA = Byte2HexStr(DecPacket[4])
											+ Byte2HexStr(DecPacket[5]);
									// nodeinfo.setId(Integer.toHexString(Integer.parseInt(StrC_NA,
									// 16)));
									nodeinfo.setId(StrC_NA);
									stringBuilder.append("\n" + "父节点地址:0x");
									for (j = 0; j < 2; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[6 + j]));
									}
									StrP_NA = Byte2HexStr(DecPacket[6])
											+ Byte2HexStr(DecPacket[7]);
									nodeinfo.setParentId(StrP_NA);
									stringBuilder.append("\n" + "所在网络ID:0x");
									for (j = 0; j < 2; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[8 + j]));
									}
									StrC_PANID = "0x"
											+ Byte2HexStr(DecPacket[8])
											+ Byte2HexStr(DecPacket[9]);

									stringBuilder.append("\n" + "协议栈版本:");
									if (Byte2HexStr(DecPacket[10]).equals("10")) {
										stringBuilder.append("ZigBee 2007");
										StrPROFILE = "ZigBee 2007";
									} else if (Byte2HexStr(DecPacket[10])
											.equals("11")) {
										stringBuilder.append("ZigBee 2007 pro");
										StrPROFILE = "ZigBee 2007";
									} else {
										stringBuilder
												.append("##Error##协议栈版本错误，请校验");
									}

									Volatage = (float) ((1.15 * (((DecPacket[22] & 0xff) << 8) + (DecPacket[23] & 0xff))) * 3.0 / 2048.0);
									stringBuilder.append("\n" + "电源电压:"
											+ floatNum.format(Volatage) + "V");
									// nodeinfo.setInfo("电源电压:" +
									// floatNum.format(Volatage) + "V");
									StrPOWER_VOL = floatNum.format(Volatage)
											+ "V";

									stringBuilder.append("\n" + "版本信息:");
									StrC_VER = "V."
											+ Byte2HexStr(DecPacket[24]);
									System.out
											.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");

									GetSensorInfoByType(DecPacket, 11,
											stringBuilder, nodeinfo);
									addANode(nodeinfo);
									break;

								case (byte) 0x03: // 查询传感器数据响应

									stringBuilder
											.append("\n------------------------------------------------------------\n");
									stringBuilder.append("查询传感器数据响应:\n");
									stringBuilder.append("节点地址:0x");
									StrC_NA = Byte2HexStr(DecPacket[4])
											+ Byte2HexStr(DecPacket[5]);
									stringBuilder.append(StrC_NA);
									NodeInfo nodeinfo1 = new NodeInfo();
									// nodeinfo.setId(Integer.toHexString(Integer.parseInt(Byte2HexStr(DecPacket[4])
									// + Byte2HexStr(DecPacket[5]), 16)));
									nodeinfo1.setId(StrC_NA);
									System.out
											.println("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM");
									GetSensorInfoByType(DecPacket, 6,
											stringBuilder, nodeinfo1);
									addANode(nodeinfo1);
									break;
								case (byte) 0x04: // 节点加入网络报告

									stringBuilder
											.append("\n------------------------------------------------------------\n");
									stringBuilder.append("节点加入网络报告:\n");
									StrC_NA = Byte2HexStr(DecPacket[4])
											+ Byte2HexStr(DecPacket[5]);
									// nodeinfo.setId(Integer.toHexString(Integer.parseInt(Byte2HexStr(DecPacket[4])
									// + Byte2HexStr(DecPacket[5]), 16)));
									stringBuilder.append("节点地址:0x" + StrC_NA);
									for (j = 0; j < 8; j++) {
										if (j < 7) {
											StrC_IEEE += Byte2HexStr(DecPacket[6 + j])
													+ "-";
										} else {
											StrC_IEEE += Byte2HexStr(DecPacket[6 + j]);
										}
									}
									stringBuilder.append("\n节点IEEE:"
											+ StrC_IEEE);

									StrP_NA = "0x" + Byte2HexStr(DecPacket[14])
											+ Byte2HexStr(DecPacket[15]);
									stringBuilder.append("\n父节点地址 :" + StrP_NA);

									for (j = 0; j < 8; j++) {
										if (j < 7) {
											StrP_IEEE += Byte2HexStr(DecPacket[16 + j])
													+ "-";
										} else {
											StrP_IEEE += Byte2HexStr(DecPacket[16 + j]);
										}
									}
									stringBuilder.append("\n父节点IEEE:"
											+ StrP_IEEE);
									break;

								case (byte) 0x11: // 查询节点信息响应

									stringBuilder
											.append("\n------------------------------------------------------------\n");
									stringBuilder.append("查询节点信息响应:\n");
									stringBuilder.append("节点地址:0x");
									StrC_NA = Byte2HexStr(DecPacket[4])
											+ Byte2HexStr(DecPacket[5]);
									// nodeinfo.setId(Integer.toHexString(Integer.parseInt(Byte2HexStr(DecPacket[4])
									// + Byte2HexStr(DecPacket[5]), 16)));
									GetNodeInfoByType(DecPacket, 6,
											stringBuilder);

									break;

								case (byte) 0x13: // 配置节点参数响应

									stringBuilder
											.append("------------------------------------------------------------\n");
									stringBuilder.append("配置节点参数响应:\n");
									stringBuilder.append("节点地址:0x");
									for (j = 0; j < 2; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[4 + j]));
									}
									StrC_NA = Byte2HexStr(DecPacket[4])
											+ Byte2HexStr(DecPacket[5]);
									// nodeinfo.setId(Integer.toHexString(Integer.parseInt(Byte2HexStr(DecPacket[4])
									// + Byte2HexStr(DecPacket[5]), 16)));
									GetArgBySetType(DecPacket, 6, stringBuilder);
									break;

								default:
									FrameErrorNum++;
									stringBuilder
											.append("------------------------------------------------------------\n");
									stringBuilder.append("未知数据帧:\n");
									stringBuilder.append("FrameType = "
											+ FrameType + "\n");
									for (j = 0; j < DecPacketLen; j++) {
										stringBuilder
												.append(Byte2HexStr(DecPacket[j]));
									}
									stringBuilder.append("\n");
									break;
								}
								// nodeinfo.setId(StrC_NA);

								// UpdateNode();
								FrameNum++;
								// mFrameNum.setText("������:"+FrameNum+"֡"+"���д�֡��"+FrameErrorNum+"֡");

							}
						}
					}
				}
			}
		}
		return stringBuilder.toString();
	}

	public Boolean IsFrameFront(final byte[] CheckData, int p) {
		if (CheckData[p] == (byte) 0xff
				&& DataCache[p + 1] == (byte) 0xff
				&& ((DataCache[p + 2] == (byte) 0x01)
						|| (DataCache[p + 2] == (byte) 0x02)
						|| (DataCache[p + 2] == (byte) 0x03)
						|| (DataCache[p + 2] == (byte) 0x04)
						|| (DataCache[p + 2] == (byte) 0x11) || (DataCache[p + 2] == (byte) 0x13))) {
			return true;
		}
		return false;
	}

	// 将一个字节数据转换为相应的十六进制字符串
	public String Byte2HexStr(final byte SrcVal) {
		StringBuilder stringBuilder = new StringBuilder("");
		int val = SrcVal & 0xFF;
		String hv = Integer.toHexString(val).toUpperCase();
		if (hv.length() < 2) {
			stringBuilder.append(0);
		}
		stringBuilder.append(hv);
		return stringBuilder.toString();
	}

	// 查询传感器数据
	private void GetSensorInfoByType(byte[] DecPacket, int p,
			StringBuilder stringBuilder, NodeInfo node) {
		// p为Type的下标，i为Data的下标
		int i = p + 1;
		// 传感器的数据
		short SensorData = 0;
		// 温度
		float Temperature = 0.0f;
		// 湿度
		float Humidity = 0.0f;
		// 电压(光敏电阻,光敏二极管,MQ-3,MQ-135,MQ-2)
		float Volatage = 0.0f;
		// X轴数据(加速度传感器，陀螺仪)
		float xVal = 0.0f;
		// Y轴数据(加速度传感器，陀螺仪)
		float yVal = 0.0f;
		// Z轴数据(加速度传感器，陀螺仪)
		float zVal = 0.0f;
		String str;
		stringBuilder.append("\n" + "����������:");
		switch (DecPacket[p]) {

		case (byte) 0x01: // LM35DZ 线性模拟温度传感器
			StrSENSOR_TYPE = "LM35DZ 线性模拟温度传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Temperature = (float) (((float) SensorData * 3.3 / 2048) / 0.01);
			StrSENSOR_DATA = "温度：" + floatNum.format(Temperature) + "℃";

			break;

		/*case (byte) 0x02: // 光敏电阻传感器
			StrSENSOR_TYPE = "光敏电阻传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Volatage = (float) ((float) SensorData * 3.3 / 2048);
			StrSENSOR_DATA = "电压:" + floatNum.format(Volatage) + "V";

			break;

		case (byte) 0x03: // 光敏二极管传感器
			StrSENSOR_TYPE = "光敏二极管传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Volatage = (float) ((float) SensorData * 3.3 / 2048);
			StrSENSOR_DATA = "电压:" + floatNum.format(Volatage) + "V";

			break;

		case (byte) 0x04: // MQ-3酒精传感器
			StrSENSOR_TYPE = "MQ-3酒精传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Volatage = (float) ((float) SensorData * 3.3 / 2048);
			StrSENSOR_DATA = "电压:" + floatNum.format(Volatage) + "V";

			break;

		case (byte) 0x05: // MQ-135空气质量传感器
			StrSENSOR_TYPE = "MQ-135空气质量传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Volatage = (float) ((float) SensorData * 3.3 / 2048);
			StrSENSOR_DATA = "电压:" + floatNum.format(Volatage) + "V";

			break;

		case (byte) 0x06: // MQ-2可燃气体传感器
			StrSENSOR_TYPE = "MQ-2可燃气体传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Volatage = (float) ((float) SensorData * 3.3 / 2048);
			StrSENSOR_DATA = "电压:" + floatNum.format(Volatage) + "V";

			break;
*/
		case (byte) 0x07: // HC-SR501人体红外传感器
			StrSENSOR_TYPE = "人体红外传感器";
		Log.i("ysysysysys", Byte2HexStr(DecPacket[i])+" "+Byte2HexStr(DecPacket[i+1]));
		
			if (Byte2HexStr(DecPacket[i]).equals("00")) {
				StrSENSOR_DATA = "无人";
			} else if (Byte2HexStr(DecPacket[i]).equals("01")) {
				StrSENSOR_DATA = "有人";
			} else {
				stringBuilder.append("##Error##传感器数据错误，请校验");
			}

			break;

		/*case (byte) 0x08: // 直流马达模块
			StrSENSOR_TYPE = "直流马达模块";

			SensorData = (short) (((DecPacket[i + 2] & 0xff) << 8) + ((DecPacket[i + 3]) & 0xff));
			StrSENSOR_DATA = "信号1占空比:"
					+ Integer.parseInt(Byte2HexStr(DecPacket[i]), 16) + "%\n"
					+ "信号2占空比:"
					+ Integer.parseInt(Byte2HexStr(DecPacket[i + 1]), 16)
					+ "%\n" + "PWM信号频率:" + SensorData + "Hz";
			if (DecPacket[12] == 0) {
				node.setMada_turnto((byte) 1);
				node.setMada_sudu(DecPacket[13]);
			} else if (DecPacket[13] == 0) {
				node.setMada_turnto((byte) 0);
				node.setMada_sudu(DecPacket[12]);
			}
			break;*/
		case (byte) 0x09: // 温湿度传感器(低精度)
			StrSENSOR_TYPE = "温湿度传感器(低精度)";

			Temperature = (float) ((DecPacket[i + 2] & 0xff) + (float) (DecPacket[i + 3] & 0xff) / 10.0);
			Humidity = (float) ((DecPacket[i] & 0xff) + (float) (DecPacket[i] & 0xff) / 10.0);
			StrSENSOR_DATA = "温度:" + floatNum.format(Temperature) + "℃\n"
					+ "湿度:" + floatNum.format(Humidity) + "%";
			if(Temperature>30)
			{
				node.setKongtiaoState((byte) 1);
			}
			else
			{
				node.setKongtiaoState((byte) 0);
			}

			
			break;

		/*case (byte) 0x0a: // ADXL345三轴数字加速度传感器
			StrSENSOR_TYPE = "ADXL345三轴数字加速度传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			xVal = (float) (SensorData * 3.9 / 1000.0);
			SensorData = (short) (((DecPacket[i + 2] & 0xff) << 8) + ((DecPacket[i + 3]) & 0xff));
			yVal = (float) (SensorData * 3.9 / 1000.0);
			SensorData = (short) (((DecPacket[i + 4] & 0xff) << 8) + ((DecPacket[i + 5]) & 0xff));
			zVal = (float) (SensorData * 3.9 / 1000.0);
			StrSENSOR_DATA = "X轴:" + floatNum.format(xVal) + "g" + "\n" + "Y轴:"
					+ floatNum.format(yVal) + "g" + "\n" + "Z轴:"
					+ floatNum.format(zVal) + "g";

			break;*/

		case (byte) 0x0b: // SHT10温湿度传感器(高精度)
			StrSENSOR_TYPE = "温湿度传感器";

			SensorData = (short) (((DecPacket[i + 2] & 0xff) << 8) + ((DecPacket[i + 3]) & 0xff));
			Temperature = (float) (-39.70 + 0.01 * SensorData);
			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Humidity = (float) (-2.0468 + 0.0367 * SensorData + (-1.5955E-6)
					* SensorData * SensorData);
			Humidity = (float) ((Temperature - 25)
					* (0.01 - 0.00008 * Humidity) + Humidity);
			StrSENSOR_DATA = "温度:" + floatNum.format(Temperature) + "℃\n"
					+ "湿度:" + floatNum.format(Humidity) + "%";
			Log.i("bbbbbbbbbbbbb", StrSENSOR_DATA);
			
			if(Temperature>30)
			{
				node.setKongtiaoState((byte) 1);
			}
			else
			{
				node.setKongtiaoState((byte) 0);
			}


			break;

		/*case (byte) 0x0c: // L3G4200D 三轴数字陀螺仪传感器
			StrSENSOR_TYPE = "L3G4200D 三轴数字陀螺仪传感器";

			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			xVal = (float) (SensorData * 8.75 / 1000.0);
			SensorData = (short) (((DecPacket[i + 2] & 0xff) << 8) + ((DecPacket[i + 3]) & 0xff));
			yVal = (float) (SensorData * 8.75 / 1000.0);
			;
			SensorData = (short) (((DecPacket[i + 4] & 0xff) << 8) + ((DecPacket[i + 5]) & 0xff));
			zVal = (float) (SensorData * 8.75 / 1000.0);
			;
			StrSENSOR_DATA = "X轴:" + floatNum.format(xVal) + "dps" + "\n"
					+ "Y轴:" + floatNum.format(yVal) + "dps" + "\n" + "Z轴:"
					+ floatNum.format(zVal) + "dps";

			break;*/
		case (byte) 0x0d:
			StrSENSOR_TYPE = "灯控制器";

			StrSENSOR_DATA = "";

			break;
		case (byte) 0x0e:
			StrSENSOR_TYPE = "智能电表";
			StrSENSOR_DATA = Integer.toHexString(((DecPacket[i] >> 4) & 0x0f)
					* 100000 + (DecPacket[i] & 0x0f) * 10000
					+ ((DecPacket[i + 1] >> 4) & 0x0f) * 1000
					+ (DecPacket[i + 1] & 0x0f) * 100
					+ ((DecPacket[i + 2] >> 4) & 0x0f) * 10
					+ (DecPacket[i + 2] & 0x0f) * 1)
					+ "."
					+ Integer.toHexString((DecPacket[i + 3] >> 4) & 0x0f)
					+ Integer.toHexString(DecPacket[i + 3] & 0x0f) + "kWh";
			break;
		/*case (byte) 0x0f:
			StrSENSOR_TYPE = "远程遥控设备";
			StrSENSOR_DATA = remoteControl(DecPacket, i);
			break;*/
		/*case (byte) 0x10:
			StrSENSOR_TYPE = "MQ_7 一氧化碳传感器";
			StrSENSOR_DATA = "";
			break;*/
		/*case (byte) 0x11:
			StrSENSOR_TYPE = "振动传感器";
			str = "0x" + Byte2HexStr(DecPacket[i]);
			if ("0x00".equals(str)) {
				StrSENSOR_DATA = "检测到振动";
			} else if ("0x01".equals(str)) {
				StrSENSOR_DATA = "未检测到振动";
			}
			break;
		case (byte) 0x12:
			StrSENSOR_TYPE = "火焰传感器";
			StrSENSOR_DATA = "";
			break;*/
		case (byte) 0x13:
			StrSENSOR_TYPE = "光照传感器";
			StrSENSOR_DATA = "光强："
					+ ((int) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff)))
					+ "lx";
			;
			if((((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff))>100)
			{
				node.setDengState((byte) 1);
			}
			else
			{
				node.setDengState((byte) 0);
			}
			break;
		case (byte) 0x14:
			StrSENSOR_TYPE = "声音传感器";
			str = "0x" + Byte2HexStr(DecPacket[i]);
			if ("0x00".equals(str)) {
				StrSENSOR_DATA = "检测到声音";
			} else if ("0x01".equals(str)) {
				StrSENSOR_DATA = "未检测到声音";
			}
			break;
		case (byte) 0x15:
			StrSENSOR_TYPE = "红外测距传感器";
			SensorData = (short) (((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff));
			Volatage = (float) ((float) SensorData * 3.3 / 2048);
			StrSENSOR_DATA = String.format("%.2f",
					(26.757 * Math.pow(Volatage, -1.236)))
					+ "cm";
			if ((((DecPacket[i] & 0xff) << 8) + ((DecPacket[i + 1]) & 0xff))>100) {
				node.setComputerState((byte) 0);
			} else  {
				node.setComputerState((byte) 1);
			} 
			break;
		case (byte) 0x16:
			StrSENSOR_TYPE = "DSM501A传感器";
			StrSENSOR_DATA = "";
			break;
		case (byte) 0x17:
			StrSENSOR_TYPE = "继电器设备";
			node.setDengState(DecPacket[p + 1]);
			if (DecPacket[p + 1] == 0)
				StrSENSOR_DATA = "低电平";
			else {
				StrSENSOR_DATA = "高电平";
			}
			break;
		case (byte) 0xfe: // 无效传感器
			StrSENSOR_TYPE = "无效传感器";
			stringBuilder.append("\n" + "无效传感器：");
			StrSENSOR_DATA = "";
			break;

		default:
			StrSENSOR_TYPE = "无效传感器";
			StrSENSOR_DATA = "";
			stringBuilder.append("\n" + "##Error##传感器类型未定义，请校验");
		}
		node.setType((byte) DecPacket[p]);
		if (!StrSENSOR_TYPE.equals("")) {
			stringBuilder.append("\n" + StrSENSOR_TYPE);
			node.setNodeName(StrSENSOR_TYPE);
		}
		if (!StrSENSOR_DATA.equals("")) {
			stringBuilder.append("\n" + StrSENSOR_DATA);
			node.setInfo(StrSENSOR_DATA);
			if (StrSENSOR_TYPE.equals("SHT10温湿度传感器(高精度)"))
				Log.i("ccccccccccccc", "xxxxxxxxxxxxxxxxxxxxxxx");
		}
	}

	private void GetNodeInfoByType(byte[] DecPacket, int p,
			StringBuilder stringBuilder) {
		int j = 0;
		// p为Type的下标，i为Data的下标
		int i = p + 1;
		// 节点运行时间
		Integer RunTime = 0;
		// 节点采集时间
		Integer GatherTime = 0;
		// 电源电压
		Float PowerVal = 0f;
		// PWM信号频率
		Integer PWMFre = 0;

		switch (DecPacket[p]) {

		case (byte) 0x01: // 节点类型
			stringBuilder.append("\n" + "查询节点类型：");
			switch (DecPacket[i]) {

			case (byte) 0x00:
				stringBuilder.append("\n"
						+ "Initialized -- not started automatically");
				break;
			case (byte) 0x01:
				stringBuilder.append("\n"
						+ "Initialized -- not connected automatically");
				break;
			case (byte) 0x02:
				stringBuilder.append("\n" + "Discovering PAN'S to join");
				break;
			case (byte) 0x03:
				stringBuilder.append("\n" + "Joining a PAN");
				break;
			case (byte) 0x04:
				stringBuilder.append("\n"
						+ "Rejoing a PAN, only for end devices");
				break;
			case (byte) 0x05:
				stringBuilder.append("\n"
						+ "Joined but not yet authenticated by trust center");
				break;
			case (byte) 0x06:
				stringBuilder.append("\n"
						+ "Start as devices after authentication");
				break;
			case (byte) 0x07:
				stringBuilder.append("\n"
						+ "Device joined, authenticated and is a router");
				break;
			case (byte) 0x08:
				stringBuilder.append("\n" + "Started as Zigbee Coordinator");
				break;
			case (byte) 0x09:
				stringBuilder.append("\n" + "Started as Zigbee Coordinator");
				break;
			case (byte) 0x0a:
				stringBuilder.append("\n"
						+ "Devices has lost information about its parent..");
				break;
			default:
				stringBuilder.append("\n" + "##Error##节点类型未定义，请校验");
			}
			break;

		case (byte) 0x02: // �ڵ�IEEE��ַ
			stringBuilder.append("\n" + "查询节点IEEE地址:");
			for (j = 0; j < 8; j++) {
				if (j < 7) {
					StrC_IEEE += Byte2HexStr(DecPacket[i + j]) + "-";
				} else {
					StrC_IEEE += Byte2HexStr(DecPacket[i + j]);
				}
			}
			stringBuilder.append(StrC_IEEE);

			break;

		case (byte) 0x03: // 节点运行时间
			stringBuilder.append("\n" + "查询节点运行时间:");
			RunTime = ((DecPacket[i] & 0xff) << 24)
					+ ((DecPacket[i + 1] & 0xff) << 16)
					+ ((DecPacket[i + 2] & 0xff) << 8)
					+ ((DecPacket[i + 3] & 0xff));
			RunTime = RunTime / 1000;
			int Hours = RunTime / 3600;
			int Minutes = (RunTime % 3600) / 60;
			int Seconds = (RunTime % 3600) % 60;
			StrRUN_TIME = Hours + "小时" + Minutes + "分钟" + Seconds + "秒";
			stringBuilder.append("\n" + "节点运行时间:" + StrRUN_TIME);
			break;

		case (byte) 0x04: // 节点PAN ID
			stringBuilder.append("\n" + "查询节点PAN ID:");
			StrC_PANID = "0x" + Byte2HexStr(DecPacket[i])
					+ Byte2HexStr(DecPacket[i + 1]);
			stringBuilder.append(StrC_PANID);
			break;

		case (byte) 0x05: // 节点协议版本
			stringBuilder.append("\n" + "查询节点协议版本:");
			if (Byte2HexStr(DecPacket[i]).equals("10")) {
				stringBuilder.append("ZigBee 2007");
				StrPROFILE = "ZigBee 2007";
			} else if (Byte2HexStr(DecPacket[i]).equals("11")) {
				stringBuilder.append("ZigBee 2007 pro");
				StrPROFILE = "ZigBee 2007 pro";
			} else {
				stringBuilder.append("\n" + "##Error##协议版本错误，请校验");
			}
			break;

		case (byte) 0x06: // 节点固件版本
			stringBuilder.append("\n" + "查询节点固件版本:");
			StrC_VER = "V." + Byte2HexStr(DecPacket[i]);
			stringBuilder.append(StrC_VER);
			break;

		case (byte) 0x07: // 节点采集时间ms
			stringBuilder.append("\n" + "查询节点采集时间:");
			GatherTime = ((DecPacket[i] & 0xff) << 8)
					+ (DecPacket[i + 1] & 0xff);
			StrNODE_GATHERTIME = GatherTime + "ms";
			stringBuilder.append("\n" + "采集时间为：" + StrNODE_GATHERTIME);
			break;

		case (byte) 0x08: // 串口通信波特率
			stringBuilder.append("\n" + "查询串口通讯波特率:");
			switch (DecPacket[i]) {

			case (byte) 0x00: // 波特率9600
				StrNODE_BAUD = "9600";
				break;
			case (byte) 0x01: // 波特率19200
				StrNODE_BAUD = "19200";
				break;
			case (byte) 0x02: // 波特率38400
				StrNODE_BAUD = "38400";
				break;
			case (byte) 0x03: // 波特率57600
				StrNODE_BAUD = "57600";
				break;
			case (byte) 0x04: // 波特率115200
				StrNODE_BAUD = "115200";
				break;
			default:
				stringBuilder.append("\n" + "##Error##设置波特率失败，请校验");
			}
			stringBuilder.append(StrNODE_BAUD);
			break;

		case (byte) 0x09: // 节点电源电压
			stringBuilder.append("\n" + "查询节点电源电压:");
			PowerVal = (float) (((3.0 / 3) / 8192.0) * (((DecPacket[i] & 0xff) << 8) + (DecPacket[i + 1] & 0xff)));
			StrPOWER_VOL = floatNum.format(PowerVal) + "V";
			stringBuilder.append(StrPOWER_VOL);
			break;

		case (byte) 0x0a: // 节点PWM参数
			stringBuilder.append("\n" + "查询节点PWM参数:");
			PWMFre = ((DecPacket[i + 1] & 0xff) << 8)
					+ ((DecPacket[i + 2]) & 0xff);
			StrSENSOR_DATA = "占空比" + Byte2HexStr(DecPacket[i]) + "%s\n"
					+ "PWM信号频率:" + PWMFre + "Hz";
			stringBuilder.append(StrSENSOR_DATA);
			break;
		case (byte) 0x0b: // 节点IO参数
			stringBuilder.append("\n" + "查询节点IO参数");
			isInputOrOutput(DecPacket, i, stringBuilder);
			break;

		case (byte) 0x0c: // 节点拓扑信息
			stringBuilder.append("\n" + "查询节点拓扑信息:");
			break;

		case (byte) 0x0d: // 节点综合信息
			stringBuilder.append("\n" + "查询节点综合信息:");
			break;

		case (byte) 0x0e: // 节点传感器信息
			stringBuilder.append("\n" + "节点传感器信息:");
			break;

		default:
			stringBuilder.append("\n" + "##Error##查询信息类型未定义，请校验");
		}
	}

	// 设置参数响应
	private void GetArgBySetType(byte[] DecPacket, int p,
			StringBuilder stringBuilder) {
		int j = 0;
		// p为Type的下标,i为起始data的下标
		int i = p + 1;
		// 定时报告时间
		Integer ReportTime = 0;
		// PWM信号频率
		Integer PWMFre = 0;

		switch (DecPacket[p]) {

		case (byte) 0x01: // 还原出厂设置
			stringBuilder.append("\n" + "还原出厂设置:");
			if (Byte2HexStr(DecPacket[i]).equals("01")) {
				stringBuilder.append("\n" + "还原后，自动重启");
			} else if (Byte2HexStr(DecPacket[i]).equals("02")) {
				stringBuilder.append("\n" + "还原后，等待手动重启");
			} else {
				stringBuilder.append("\n" + "##Error##还原设置参数错误，请校验");
			}
			break;

		case (byte) 0x02: // 设置定时报告时间
			stringBuilder.append("\n" + "设置定时报告时间(ms):");
			for (j = 0; j < 2; j++) {
				stringBuilder.append(Byte2HexStr(DecPacket[i + j]));
			}

			ReportTime = ((DecPacket[i] & 0xff) << 8)
					+ ((DecPacket[i + 1]) & 0xff);
			stringBuilder.append("\n" + "定时报告时间:" + ReportTime + "ms");
			break;

		case (byte) 0x03: // 设置串口通信波特率
			stringBuilder.append("\n" + "设置串口通信波特率:");
			switch (DecPacket[i]) {

			case (byte) 0x00: // 波特率9600
				stringBuilder.append("\n" + "设置波特率为9600成功");
				break;
			case (byte) 0x01: // 波特率19200
				stringBuilder.append("\n" + "设置波特率为19200成功");
				break;
			case (byte) 0x02: // 波特率38400
				stringBuilder.append("\n" + "设置波特率为38400成功");
				break;
			case (byte) 0x03: // 波特率57600
				stringBuilder.append("\n" + "设置波特率为57600成功");
				break;
			case (byte) 0x04: // 波特率115200
				stringBuilder.append("\n" + "设置波特率为115200成功");
				break;
			default:
				stringBuilder.append("\n" + "##Error##设置波特率失败，请校验");
			}
			break;

		case (byte) 0x04: // 设置PWM控制参数
			stringBuilder.append("\n" + "设置PWM控制参数:");
			stringBuilder.append("\n" + "PWM信号占空比:");
			stringBuilder.append(Byte2HexStr(DecPacket[i]) + "%s");
			PWMFre = ((DecPacket[i + 1] & 0xff) << 8)
					+ ((DecPacket[i + 2]) & 0xff);
			stringBuilder.append("\n" + "PWM信号频率:" + PWMFre + "Hz");
			break;

		default:
			stringBuilder.append("\n" + "##Error##设置类型未定义，请校验");
		}
	}

	public void isInputOrOutput(final byte[] DecPacket, int i,
			StringBuilder stringBuilder) {

		String StrPIN_NUM = "";
		for (int j = 0; j < 2; j++) {
			if (j == 0) {
				StrPIN_NUM = "PIN0.";
			} else {
				StrPIN_NUM = "PIN1.";
			}
			for (int n = 0; n < 4; n++) {
				if (((byte) DecPacket[i] & 0x01) != 0) {
					stringBuilder.append("\n" + StrPIN_NUM + (n + 4) + "为输出口");
					if (((byte) DecPacket[i + 1] & 0x01) != 0) {
						stringBuilder.append("\n" + StrPIN_NUM + (n + 4)
								+ "高电平");
					} else {
						stringBuilder.append("\n" + StrPIN_NUM + (n + 4)
								+ "低电平");
					}
				} else {
					stringBuilder.append("\n" + StrPIN_NUM + (n + 4) + "为输入口");
				}
				DecPacket[i] = (byte) ((DecPacket[i] & 0xff) >> 1);
				DecPacket[i + 1] = (byte) ((DecPacket[i + 1] & 0xff) >> 1);
			}
		}
	}

	public void addANode(NodeInfo node) {

		node.setLastRecordTime(System.currentTimeMillis());
		if (!replaceANodeInList(nodelist, node))
			nodelist.add(node);

		if (node.getType() == 0x13) {

			Message msg = new Message();
			msg.what = 0x2222;
			msg.obj = node;
			if (ControlInterface.handler != null
					&& ControlInterface.currentUiName.equals("光照传感器")) {

				if (ControlInterface.nodeinfo != null
						&& ControlInterface.nodeinfo.getId().equals(
								node.getId()))
					ControlInterface.handler.sendMessage(msg);

			}
		}
		if (node.getType() == 0x0b) {

			Message msg = new Message();
			msg.what = 0x2223;
			msg.obj = node;
			if (ControlInterface.handler != null
					&& ControlInterface.currentUiName.equals("温湿度传感器"))
				ControlInterface.handler.sendMessage(msg);

		}
		if (node.getType() == 0x15) {

			Message msg = new Message();
			msg.what = 0x2224;
			msg.obj = node;
			if (ControlInterface.handler != null
					&& ControlInterface.currentUiName.equals("红外测距传感器"))
				ControlInterface.handler.sendMessage(msg);

		}
		if (node.getType() == 0x14) {

			Message msg = new Message();
			msg.what = 0x2225;
			msg.obj = node;
			if (ControlInterface.handler != null
					&& ControlInterface.currentUiName.equals("shengyin传感器"))
				ControlInterface.handler.sendMessage(msg);

		}
		Iterator<NodeInfo> litrator = nodelist.iterator();
		while (litrator.hasNext()) {
			if (System.currentTimeMillis()
					- litrator.next().getLastRecordTime() > 10000) {
				litrator.remove();
			}
		}

		BSLActivity.mainHandler.sendEmptyMessage(0x1111);
	}

	public OutputStream getOutPutStream() {
		return outputStream;
	}

	public Socket getSocket() {
		return socket;
	}

	private boolean replaceANodeInList(List<NodeInfo> list, NodeInfo nodeInfo) {
		int temp = -1;
		if (list != null && list.size() > 0) {

			for (int i = 0; i < list.size(); i++) {
				NodeInfo nodeInfo2 = list.get(i);
				if (nodeInfo2.equals(nodeInfo)) {
					temp = i;
				}
			}
			if (temp != -1) {
				list.set(temp, nodeInfo);
				return true;
			}
		}
		return false;
	}

	/*public String remoteControl(byte[] DecPacket, int i) {
		String str = "0x" + Byte2HexStr(DecPacket[i]);
		String deviceName = "";
		if ("0x00".equals(str)) {
			deviceName = "电视机+机顶盒";
		} else if ("0x01".equals("str")) {
			deviceName = "空调";
		} else if ("0x02".equals(str)) {
			deviceName = "DVD或高清播放器";
		} else if ("0x03".equals(str)) {
			deviceName = "电灯";
		} else if ("0x04".equals(str)) {
			deviceName = "电风扇";
		} else if ("0xfe".equals(str)) {
			deviceName = "无效设备";
		}
		return deviceName;
	}*/
}