package com.example.pl2303hx;

import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

public class Data_SR {
	
	private UsbManager mUsbManager;
	private UsbDevice mUsbDevice;
	private Context context;
	private UsbInterface mUsbInterface;
	private UsbDeviceConnection mUsbDeviceConnection;
	private UsbEndpoint mUE00,mUE01,mUE02;
	boolean SND = true;
	boolean data_SND = true;
	byte[] setup_ = new byte[7];
	public boolean latch = false;
	private TextView tv1;
	private StringBuilder mStringBuilder = null;
	
	public Data_SR(UsbManager mUsbManager,UsbDevice mUsbDevice,Context context,TextView tv1){
		this.mUsbManager = mUsbManager;
		this.mUsbDevice = mUsbDevice;
		this.context = context;
		this.tv1 = tv1;
	}
	
	public boolean send(final String data){
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				byte[] data_ = data.getBytes();
				int ret = mUsbDeviceConnection.bulkTransfer(mUE01, data_, data_.length, 0);
				if(ret<0)data_SND=false;
			}}).start();
		
		return data_SND;
	}
	
	public boolean initialize(){
		if(!mUsbManager.hasPermission(mUsbDevice))return false;
		mUsbInterface = mUsbDevice.getInterface(0); if(mUsbInterface==null)return false;
		mUE00 = mUsbInterface.getEndpoint(0); if((mUE00.getType()!=UsbConstants.USB_ENDPOINT_XFER_INT)||(mUE00.getDirection()!=UsbConstants.USB_DIR_IN))return false; //interrupt
		mUE01 = mUsbInterface.getEndpoint(1); if((mUE01.getType()!=UsbConstants.USB_ENDPOINT_XFER_BULK)||(mUE01.getDirection()!=UsbConstants.USB_DIR_OUT))return false; //TX
		mUE02 = mUsbInterface.getEndpoint(2); if((mUE02.getType()!=UsbConstants.USB_ENDPOINT_XFER_BULK)||(mUE02.getDirection()!=UsbConstants.USB_DIR_IN))return false; //RX
		mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);if(mUsbDeviceConnection==null)return false;
		if(!mUsbDeviceConnection.claimInterface(mUsbInterface, true))return false;
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					byte[] buffer = new byte[1];
					vendor_read(0x8484,0,buffer,1);
					vendor_write(0x0404,0,null,0);
					vendor_read(0x8484,0,buffer,1);
					vendor_read(0x8383,0,buffer,1);
					vendor_read(0x8484,0,buffer,1);
					vendor_write(0x0404,1,null,0);
					vendor_read(0x8484,0,buffer,1);
					vendor_read(0x8383,0,buffer,1);
					vendor_write(0,1,null,0);
					vendor_write(1,0,null,0);
					vendor_write(2,0x44,null,0);
				}catch(Exception e){
					e.printStackTrace();
					SND = false;
				}
			}}).start();
		
		setup_[0] = (byte)(9600 & 0xff);
		setup_[1] = (byte)((9600 >> 8) & 0xff);
		setup_[2] = (byte)((9600 >> 16) & 0xff);
		setup_[3] = (byte)((9600 >> 24) & 0xff);
		setup_[4] = (byte)0;
		setup_[5] = (byte)0;
		setup_[6] = (byte)8;
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					int ret0 =mUsbDeviceConnection.controlTransfer(0xa1, 0x21, 0, 0, new byte[7], 7, 100);if(ret0<7)SND=false;
					int ret = mUsbDeviceConnection.controlTransfer(0x21, 0x20, 0, 0, setup_, 7, 100);if(ret<7)SND=false;
					int ret1 = mUsbDeviceConnection.controlTransfer(0x21, 0x23, 0x0000, 0, null, 0, 100);if(ret1<0)SND=false;
					vendor_write(0x0,0x0,null,0);
					int ret2 = mUsbDeviceConnection.controlTransfer(0x21, 0x22, (0-0x01), 0, null, 0, 100);if(ret2<0)SND=false;
					int ret3 = mUsbDeviceConnection.controlTransfer(0x21, 0x22, (0-0x02), 0, null, 0, 100);if(ret3<0)SND=false;
				}catch(Exception e){
					e.printStackTrace();
					SND = false;
				}
			}}).start();
			
		return SND;
	}
	
	private void vendor_write(int value,int index,byte[] buffer,int length)throws Exception {
		int ret = mUsbDeviceConnection.controlTransfer(0x40, 0x01, value, index, buffer, length, 100);
		if(ret<0)throw new Exception("Vendor write request failed! Value: 0x"+ String.format("%04X", value) + " Index: " + index + "Length: " + length +" Return: " + ret);
	}
	
	private void vendor_read(int value,int index,byte[] buffer,int length)throws Exception {
		int ret = mUsbDeviceConnection.controlTransfer(0xc0,0x01, value, index, buffer, length, 100);
		if(ret<0)throw new Exception("Vendor read request failed! Value: 0x"+ String.format("%04X", value) + " Index: " + index + "Length: " + length +" Return: " + ret);
	}
	
	public void data_read(){
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				while(latch){
					read_data();
				}
			}}).start();
		
	}
	
	private void read_data(){
		int totalBytesRead = 0;
		int readPos = 1;
		int length = 28;
		int ret = 0;
		int offset = 0;
		
		int max_size = mUE02.getMaxPacketSize();
		byte[] buffer = new byte[max_size];
		byte[] readBuffer = new byte[max_size];
		
		/*while(totalBytesRead < length){
			if(readPos>ret-1){
				ret = mUsbDeviceConnection.bulkTransfer(mUE02, buffer, 4096, 100);
				if(ret>0)readPos=0;
			}
			if(ret>0){
				System.arraycopy(readBuffer, readPos, buffer, 0, ret-readPos);
				offset = offset + ret - readPos;
				totalBytesRead = totalBytesRead + ret -readPos;
				readPos = ret -readPos;
			}
		}*/
		if(mUsbDeviceConnection.bulkTransfer(mUE02, buffer, 4096, 100)>0){
			for(int i=2;i<4096;i++){
				mStringBuilder.append((char)(buffer[i]));
				Message msg = new Message();
				msg.what = 2;
				msg.obj = mStringBuilder;
				handler.sendMessage(msg);
			}
		}
		
		
		/*if(ret>0){
			Message msg = new Message();
			msg.what = 1;
			msg.obj = buffer;
			handler.sendMessage(msg);
		}*/
	}
	
	public Handler handler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
			case 1:
				try {
					byte[] buffer = (byte[]) msg.obj;
					Toast.makeText(context, ""+byte2String(buffer), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 2:
				tv1.setText(mStringBuilder);
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	public int byte2String(byte[] buffer){
		int i=0;
		for(i=0;i<buffer.length;i++){
			if(String.valueOf(buffer[i]).equals("")){
				break;
			}
		}
		return i;
	}
	
	public void close(){
		latch=false;
		mUsbDeviceConnection.releaseInterface(mUsbInterface);
		mUsbDeviceConnection.close();
		try{
			Thread.sleep(500);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
