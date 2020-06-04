package com.example.comtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.bjw.ComAssistant.ComAssistantActivity;
import com.bjw.ComAssistant.MyFunc;
import com.bjw.ComAssistant.SerialHelper;
import com.bjw.bean.AssistBean;
import com.bjw.bean.ComBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Queue;

import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity {
    SerialControl ComA;
    AssistBean AssistData;//用于界面数据序列化和反序列化
    SerialPortFinder mSerialPortFinder;//串口设备搜索
    DispQueueThread DispQueue;//刷新显示线程
    //----------------------------------------------------刷新显示线程
    private class DispQueueThread extends Thread{
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();
        @Override
        public void run() {
            super.run();
            while(!isInterrupted()) {
                final ComBean ComData;
                while((ComData=QueueList.poll())!=null)
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            DispRecData(ComData);
                        }
                    });
                    try
                    {
                        Thread.sleep(10);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData){
            QueueList.add(ComData);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        setControls();
    }

    private void setControls() {
        mSerialPortFinder= new SerialPortFinder();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        ComA.setPort("/dev/ttyS1");
        ComA.setBaudRate(9600);
        OpenComPort(ComA);


    }


    //----------------------------------------------------打开串口
    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
            ShowMessage("打开串口失败:没有串口读/写权限!");
        } catch (IOException e) {
            ShowMessage("打开串口失败:未知错误!");
        } catch (InvalidParameterException e) {
            ShowMessage("打开串口失败:参数错误!");
        }
    }
    //------------------------------------------显示消息
    private void ShowMessage(String sMsg)
    {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        CloseComPort(ComA);
        super.onDestroy();

    }



    //----------------------------------------------------关闭串口
    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    //----------------------------------------------------串口控制类
    private class SerialControl extends SerialHelper {

        //		public SerialControl(String sPort, String sBaudRate){
//			super(sPort, sBaudRate);
//		}
        public SerialControl(){
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData)
        {
            //数据接收量大或接收时弹出软键盘，界面会卡顿,可能和6410的显示性能有关
            //直接刷新显示，接收数据量大时，卡顿明显，但接收与显示同步。
            //用线程定时刷新显示可以获得较流畅的显示效果，但是接收数据速度快于显示速度时，显示会滞后。
            //最终效果差不多-_-，线程定时刷新稍好一些。
//           DispRecData(ComRecData);
            DispQueue.AddQueue(ComRecData);//线程定时刷新显示(推荐)
			/*
			runOnUiThread(new Runnable()//直接刷新显示
			{
				public void run()
				{
					DispRecData(ComRecData);
				}
			});*/
        }
    }

    private void DispRecData(ComBean ComRecData){
        StringBuilder sMsg=new StringBuilder();
        sMsg.append(ComRecData.sRecTime);
        sMsg.append("[");
        sMsg.append(ComRecData.sComPort);
        sMsg.append("]");
        sMsg.append("[Txt] ");
        sMsg.append(new String(ComRecData.bRec));
        sMsg.append("\r\n");
        Log.e("hudie",sMsg.toString());

    }
}


