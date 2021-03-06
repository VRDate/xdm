package xdman.mediaconversion;

import xdman.Config;
import xdman.util.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaInfoExtractor {
	Pattern pattern1, pattern2;
	boolean stop;
	Process proc;

	public void stop() {
		stop=true;
		if (proc != null) {
			try {
				proc.destroy();
			} catch (Exception e) {
			}
		}
	}

	public MediaInfoExtractor() {
		String str1="Duration:\\s+(\\d+:\\d+:\\d+)    "+"Stream .*, ([0-9]+x[0-9]+)";
		Logger.log(str1);
		pattern1 = Pattern.compile("Duration:\\s+([0-9]+:[0-9]+:[0-9]+)");
		pattern2 = Pattern.compile("Stream .*, ([0-9]+x[0-9]+)");

		//Logger.log(pattern1.matcher("Duration: 00:07:38.36, start: ").m);
	}

	public MediaFormatInfo getInfo(String file) {
		File ffMpegFile = FFmpeg.getFFMpegFile();
		if (!FFmpeg.isFFmpegInstalled(ffMpegFile)) {
			IOException ioException = new IOException(String.format("FFMpeg not installed %s",
					ffMpegFile != null
							? ffMpegFile.getAbsolutePath()
							: null));
			Logger.log(ioException);
			return null;
		}
		File f = new File(file);
		File tmpOutput = new File(Config.getInstance().getTemporaryFolder(), UUID.randomUUID().toString());
		File tmpImgFile = new File(Config.getInstance().getTemporaryFolder(), UUID.randomUUID().toString() + ".jpg");
		if (!f.exists())
			return null;
		if (stop) {
			return null;
		}
		try {
			List<String> args = new ArrayList<String>();
			args.add(ffMpegFile.getAbsolutePath());
			args.add("-i");
			args.add(f.getAbsolutePath());
			args.add("-vf");
			args.add("scale=64:-1");
			args.add("-vframes");
			args.add("1");
			args.add("-f");
			args.add("image2");
			args.add(tmpImgFile.getAbsolutePath());
			args.add("-y");
			
			String str2="";
			for(String s:args) {
				str2+=" "+s;
			}

			Logger.log(str2);

			ProcessBuilder pb = new ProcessBuilder(args);
			//pb.redirectErrorStream(false);
			pb.redirectError(tmpOutput);
			proc = pb.start();

			int ret=proc.waitFor();
			Logger.log("ret:", ret);
			if(stop) {
				return null;
			}
			MediaFormatInfo info = new MediaFormatInfo();
			info.thumbnail = new ImageIcon(tmpImgFile.getAbsolutePath());
			byte[] array = Files.readAllBytes(tmpOutput.toPath());
			String str = new String(array, StandardCharsets.UTF_8);
			Logger.log(str);
			Matcher matcher1 = pattern1.matcher(str);
			Matcher matcher2 = pattern2.matcher(str);
			if (matcher1.find()) {
				info.duration = matcher1.group(1);
				Logger.log("Match:", info.duration);
			}else {
				Logger.log("no match");
			}
			if (matcher2.find()) {
				info.resolution = matcher2.group(1);
				Logger.log("Match:", info.resolution);
			}
			if(stop) {
				return null;
			}
			return info;
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			tmpOutput.delete();
			tmpImgFile.delete();
		}
		return null;
	}
}
