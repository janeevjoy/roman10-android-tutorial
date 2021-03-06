package feipeng.yacamcorder;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import feipeng.yacamcorder.R.*;

/***
 *  TODO: 1. sound on/off
 *  2. resolution change
 * @author roman10
 *
 */

public class Main extends Activity implements SurfaceHolder.Callback {
    private SurfaceView prSurfaceView;
    private Button prStartBtn;
    private Button prSettingsBtn;
    private boolean prRecordInProcess;
    private SurfaceHolder prSurfaceHolder;
    private Camera prCamera;
	private final String cVideoFilePath = "/sdcard/r10videocam/";
    
	private Context prContext;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        prContext = this.getApplicationContext();
        setContentView(R.layout.main);
        Utils.createDirIfNotExist(cVideoFilePath);
        prSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        prStartBtn = (Button) findViewById(R.id.main_btn1);
        prSettingsBtn = (Button) findViewById(R.id.main_btn2);
        prRecordInProcess = false;
        prStartBtn.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				if (prRecordInProcess == false) {
					startRecording();
				} else {
					stopRecording();
				}
			}
		});
        prSettingsBtn.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				Intent lIntent = new Intent();
				lIntent.setClass(prContext, feipeng.yacamcorder.SettingsDialog.class);
				startActivityForResult(lIntent, REQUEST_DECODING_OPTIONS);
			}
		});
        prSurfaceHolder = prSurfaceView.getHolder();
        prSurfaceHolder.addCallback(this);
        prSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		prMediaRecorder = new MediaRecorder();
    }

	//@Override
	public void surfaceChanged(SurfaceHolder _holder, int _format, int _width, int _height) {
		Camera.Parameters lParam = prCamera.getParameters();
//		//lParam.setPreviewSize(_width, _height);
//		//lParam.setPreviewSize(320, 240);
//		lParam.setPreviewFormat(PixelFormat.JPEG);
		prCamera.setParameters(lParam);
		try {
			prCamera.setPreviewDisplay(_holder);
			prCamera.startPreview();
			//prPreviewRunning = true;
		} catch (IOException _le) {
			_le.printStackTrace();
		}
	}

	//@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		prCamera = Camera.open();
		if (prCamera == null) {
			Toast.makeText(this.getApplicationContext(), "Camera is not available!", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	//@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		if (prRecordInProcess) {
			stopRecording();
		} else {
			prCamera.stopPreview();
		}
		prMediaRecorder.release();
		prMediaRecorder = null;
		prCamera.release();
		prCamera = null;
	}
	
	private MediaRecorder prMediaRecorder;
	private final int cMaxRecordDurationInMs = 30000;
	private final long cMaxFileSizeInBytes = 5000000;
	private final int cFrameRate = 20;
	private File prRecordedFile;
	
	private void updateEncodingOptions() {
		if (prRecordInProcess) {
			stopRecording();
			startRecording();
			Toast.makeText(prContext, "Recording restarted with new options!", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(prContext, "Recording options updated!", Toast.LENGTH_SHORT).show();
		}
	}
	
	private boolean startRecording() {
		prCamera.stopPreview();
		try {
			prCamera.unlock();
			prMediaRecorder.setCamera(prCamera);
			//set audio source as Microphone, video source as camera
			//state: Initial=>Initialized
			prMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			prMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			//set the file output format: 3gp or mp4
			//state: Initialized=>DataSourceConfigured
			String lVideoFileFullPath;
			String lDisplayMsg = "Current container format: ";
			if (Utils.puContainerFormat == SettingsDialog.cpu3GP) {
				lDisplayMsg += "3GP\n";
				lVideoFileFullPath = ".3gp";
				prMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			} else if (Utils.puContainerFormat == SettingsDialog.cpuMP4) {
				lDisplayMsg += "MP4\n";
				lVideoFileFullPath = ".mp4";
				prMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			} else {
				lDisplayMsg += "3GP\n";
				lVideoFileFullPath = ".3gp";
				prMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			}
			//the encoders: 
			//audio: AMR-NB
//			prMediaRecorder.setAudioEncoder(AudioEncoder.AMR_NB);
//			prMediaRecorder.setAudioEncoder(AudioEncoder.AMR_WB);
			prMediaRecorder.setAudioEncoder(AudioEncoder.AAC);
//			prMediaRecorder.setAudioEncoder(AudioEncoder.AAC_ELD);
//			prMediaRecorder.setAudioEncoder(AudioEncoder.HE_AAC);
			//video: H.263, MP4-SP, or H.264
			//prMediaRecorder.setVideoEncoder(VideoEncoder.H263);
			//prMediaRecorder.setVideoEncoder(VideoEncoder.MPEG_4_SP);
			lDisplayMsg += "Current encoding format: ";
			if (Utils.puEncodingFormat == SettingsDialog.cpuH263) {
				lDisplayMsg += "H263\n";
				prMediaRecorder.setVideoEncoder(VideoEncoder.H263);
			} else if (Utils.puEncodingFormat == SettingsDialog.cpuMP4_SP) {
				lDisplayMsg += "MPEG4-SP\n";
				prMediaRecorder.setVideoEncoder(VideoEncoder.MPEG_4_SP);
			} else if (Utils.puEncodingFormat == SettingsDialog.cpuH264) {
				lDisplayMsg += "H264\n";
				prMediaRecorder.setVideoEncoder(VideoEncoder.H264);
			} else {
				lDisplayMsg += "H263\n";
				prMediaRecorder.setVideoEncoder(VideoEncoder.H263);
			}
			lVideoFileFullPath = cVideoFilePath + String.valueOf(System.currentTimeMillis()) + lVideoFileFullPath;
			prRecordedFile = new File(lVideoFileFullPath);
			prMediaRecorder.setOutputFile(prRecordedFile.getPath());
			if (Utils.puResolutionChoice == SettingsDialog.cpuRes176) {
				prMediaRecorder.setVideoSize(176, 144); 
			} else if (Utils.puResolutionChoice == SettingsDialog.cpuRes320) {
				prMediaRecorder.setVideoSize(320, 240);
			} else if (Utils.puResolutionChoice == SettingsDialog.cpuRes720) {
				prMediaRecorder.setVideoSize(720, 480);
			} 
			Toast.makeText(prContext, lDisplayMsg, Toast.LENGTH_LONG).show();
			prMediaRecorder.setVideoFrameRate(cFrameRate);
			prMediaRecorder.setPreviewDisplay(prSurfaceHolder.getSurface());
			prMediaRecorder.setMaxDuration(cMaxRecordDurationInMs);
			prMediaRecorder.setMaxFileSize(cMaxFileSizeInBytes);
			//prepare for capturing
			//state: DataSourceConfigured => prepared
			prMediaRecorder.prepare();
			//start recording
			//state: prepared => recording
			prMediaRecorder.start();
			prStartBtn.setText("Stop");
			prRecordInProcess = true;
			return true;
		} catch (IOException _le) {
			_le.printStackTrace();
			return false;
		}
	}
	
	private void stopRecording() {
		prMediaRecorder.stop();
		prMediaRecorder.reset();
		try {
			prCamera.reconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		prStartBtn.setText("Start");
		prRecordInProcess = false;
		prCamera.startPreview();
	}
	
	private static final int REQUEST_DECODING_OPTIONS = 0;
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	switch (requestCode) {
    	case REQUEST_DECODING_OPTIONS:
    		if (resultCode == RESULT_OK) {
    			updateEncodingOptions();
    		}
    		break;
    	}
	}
}