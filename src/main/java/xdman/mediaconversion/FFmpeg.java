package xdman.mediaconversion;

import xdman.Config;
import xdman.Main;
import xdman.util.FileUtils;
import xdman.util.Logger;
import xdman.util.StringUtils;
import xdman.util.os.OSUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FFmpeg {
	public final static int FF_NOT_FOUND = 10, FF_LAUNCH_ERROR = 20, FF_CONVERSION_FAILED = 30, FF_SUCCESS = 0;
	private MediaFormat outformat;
	private MediaConversionListener listener;
	private boolean copy;
	private List<String> inputFiles;
	private String outputFile;
	private boolean hls;
	private long totalDuration = 0;
	private Process proc;
	private int ffExitCode;
	private String preset = "ultrafast";
	private String volume;
	private boolean useHwAccel;

	public FFmpeg(List<String> inputFiles, String outputFile, MediaConversionListener listener, MediaFormat outformat,
	              boolean copy) {
		this.inputFiles = inputFiles;
		this.outputFile = outputFile;
		this.listener = listener;
		this.outformat = outformat;
		this.copy = copy;
	}

	public int convert() {
		File ffMpegFile = FFmpeg.getFFMpegFile();
		if (!FFmpeg.isFFmpegInstalled(ffMpegFile)) {
			IOException ioException = new IOException(String.format("FFMpeg not installed %s",
					ffMpegFile != null
							? ffMpegFile.getAbsolutePath()
							: null));
			Logger.log(ioException);
			return FF_NOT_FOUND;
		}
		BufferedReader bufferedReader = null;
		try {
			Logger.log("Outformat:", outformat);

			List<String> args = new ArrayList<String>();
			args.add(ffMpegFile.getAbsolutePath());

			if (useHwAccel) {
				args.add("-hwaccel");
				args.add("auto");
			}

			if (hls) {
				args.add("-f");
				args.add("concat");
				args.add("-safe");
				args.add("0");
			}

			for (int i = 0; i < inputFiles.size(); i++) {
				args.add("-i");
				args.add(inputFiles.get(i));
			}

			if (copy) {
				args.add("-acodec");
				args.add("copy");
				args.add("-vcodec");
				args.add("copy");
			} else {
				// args.add("-f");
				// args.add(outformat.getFormat());
				if (outformat.getResolution() != null) {
					args.add("-s");
					args.add(outformat.getResolution());
				}
				if (outformat.getVideo_codec() != null) {
					args.add("-vcodec");
					args.add(outformat.getVideo_codec());
				}
				if (outformat.getVideo_bitrate() != null) {
					args.add("-b:v");
					args.add(outformat.getVideo_bitrate());
				}
				if (outformat.getFramerate() != null) {
					args.add("-r");
					args.add(outformat.getFramerate());
				}
				if (outformat.getAspectRatio() != null) {
					args.add("-aspect");
					args.add(outformat.getAspectRatio());
				}
				if (outformat.getVideo_param_extra() != null) {
					String[] arr = outformat.getVideo_param_extra().split(" ");
					if (arr.length > 0) {
						args.addAll(Arrays.asList(arr));
					}
				} else {
					if ("libx264".equals(outformat.getVideo_codec())) {
						args.add("-profile:v");
						args.add("baseline");
					}
				}

				if (outformat.getAudio_codec() != null) {
					args.add("-acodec");
					args.add(outformat.getAudio_codec());
				}
				if (outformat.getAudio_bitrate() != null) {
					args.add("-b:a");
					args.add(outformat.getAudio_bitrate());
				}
				if (isNumeric(outformat.getSamplerate())) {
					args.add("-ar");
					args.add(outformat.getSamplerate());
				}
				if (isNumeric(outformat.getAudio_channel())) {
					args.add("-ac");
					args.add(outformat.getAudio_channel());
				}
				if (outformat.getAudio_extra_param() != null) {
					String[] arr = outformat.getAudio_extra_param().split(" ");
					if (arr.length > 0) {
						args.addAll(Arrays.asList(arr));
					}
				}
				if (volume != null) {
					args.add("-filter:a");
					args.add("volume=" + volume);
				}
			}

			// if (!copy) {
			// args.add("-preset");
			// args.add(preset);
			// }

			// if (outformat.isAudioOnly()) {
			// if (outformat.getWidth() > 0) {
			// args.add("-b:a");
			// args.add(outformat.getWidth() + "k");
			// } else if (copy) {
			// args.add("-acodec");
			// args.add("copy");
			// }
			// } else {
			// if (outformat.getWidth() > 0) {
			// args.add("-vf");
			// args.add("scale=" + outformat.getWidth() + ":" + outformat.getHeight());
			// // args.add("scale=w=" + outformat.getWidth() + ":h=" +
			// // outformat.getHeight()
			// // + ":force_original_aspect_ratio=decrease");
			// } else if (copy) {
			// args.add("-acodec");
			// args.add("copy");
			// args.add("-vcodec");
			// args.add("copy");
			// }
			// }

			args.add(outputFile);
			args.add("-y");

			for (String s : args) {
				Logger.log("@ffmpeg_args:", s);
			}

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream(true);
			proc = pb.start();

			bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()), 1024);
			String ln;
			while ((ln = bufferedReader.readLine()) != null) {
				try {
					String text = ln.trim();
					processOutput(text);
				} catch (Exception e) {
					Logger.log(e);
				}
			}

			ffExitCode = proc.waitFor();
			return ffExitCode == 0 ? FF_SUCCESS : FF_CONVERSION_FAILED;
		} catch (Exception e) {
			Logger.log(e);
			return FF_LAUNCH_ERROR;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (Exception e2) {
					Logger.log(e2);
				}
			}
		}
	}

	public static boolean isFFmpegInstalled() {
		File ffMpegFile = getFFMpegFile();
		boolean isFFmpegInstalled = isFFmpegInstalled(ffMpegFile);
		return isFFmpegInstalled;
	}

	public static boolean isFFmpegInstalled(File ffMpegFile) {
		boolean isFFmpegInstalled = ffMpegFile != null
				&& ffMpegFile.exists();
		Logger.log("is FFMpeg Installed",
				ffMpegFile != null
						? ffMpegFile.getAbsolutePath()
						: null,
				isFFmpegInstalled);
		return isFFmpegInstalled;
	}

	public static File getFFMpegFile() {
		String ffMpeg = getFFMpeg();
		File dataFFmpegFile = new File(Config.getInstance().getDataFolder(),
				ffMpeg);
		if (dataFFmpegFile.exists()) {
			return dataFFmpegFile;
		}
		File jarFile = FileUtils.getJarFile(Main.class);
		File jarFFmpegFile = new File(jarFile.getParentFile(),
				ffMpeg);
		File ffmpegFile = jarFFmpegFile.exists() ? jarFFmpegFile
				: null;
		return ffmpegFile;
	}

	public static String getFFMpeg() {
		String ffmpeg = OSUtils.getEXEFileName("ffmpeg");
		return ffmpeg;
	}

	public void setHls(boolean hls) {
		this.hls = hls;
	}

	public void setHLSDuration(float totalDuration) {
		this.totalDuration = (long) totalDuration;
	}

	private long parseDuration(String dur) {
		long duration = 0;
		String[] arr = dur.split(":");
		String s = arr[0].trim();
		if (!StringUtils.isNullOrEmpty(s)) {
			duration = Integer.parseInt(s, 10) * 3600;
		}
		s = arr[1].trim();
		if (!StringUtils.isNullOrEmpty(s)) {
			duration += Integer.parseInt(arr[1].trim(), 10) * 60;
		}
		s = arr[2].split("\\.")[0].trim();
		if (!StringUtils.isNullOrEmpty(s)) {
			duration += Integer.parseInt(s, 10);
		}
		return duration;
	}

	private void processOutput(String text) {
		if (StringUtils.isNullOrEmpty(text)) {
			return;
		}
		if (totalDuration > 0) {
			if (text.startsWith("frame=") && text.contains("time=")) {
				int index1 = text.indexOf("time");
				index1 = text.indexOf('=', index1);
				int index2 = text.indexOf("bitrate=");
				String dur = text.substring(index1 + 1, index2).trim();
				Logger.log("Parsing duration:", dur);
				long t = parseDuration(dur);
				Logger.log("Duration:", t, "Total duration:", totalDuration);
				int prg = (int) ((t * 100) / totalDuration);
				Logger.log("ffmpeg prg:", prg);
				listener.progress(prg);
			}
		}

		if (totalDuration == 0) {
			if (text.startsWith("Duration:")) {
				try {
					int index1 = text.indexOf("Duration");
					index1 = text.indexOf(':', index1);
					int index2 = text.indexOf(",", index1);
					String dur = text.substring(index1 + 1, index2).trim();
					Logger.log("Parsing duration:", dur);
					totalDuration = parseDuration(dur);
					Logger.log("Total duration:", totalDuration);
				} catch (Exception e) {
					Logger.log(e);
					totalDuration = -1;
				}
			}
		}
	}

	public void stop() {
		try {
			if (proc.isAlive()) {
				proc.destroy();
			}
		} catch (Exception e) {
		}
	}

	public int getFfExitCode() {
		return ffExitCode;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

	private boolean isNumeric(String s) {
		try {
			Double.parseDouble(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean isUseHwAccel() {
		return useHwAccel;
	}

	public void setUseHwAccel(boolean useHwAccel) {
		this.useHwAccel = useHwAccel;
	}
}
