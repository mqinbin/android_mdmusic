package com.qinbin.mdmusic.engine;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.monitorenter.cpdetector.io.ByteOrderMarkDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;

public class Lrc {
	List<LrcLine> lines;

	LrcLine lastFindLrcLine;

	public static class LrcLine implements Comparable<LrcLine> {
		long startTime;
		String content;
		long endTime;

		public boolean between(long duration) {
			if (duration >= startTime && duration <= endTime) {
				return true;
			} else {
				return false;
			}
		}

		// 6暴露方法供调用 : 将此方法隐藏
		public long getStartTime() {
			return startTime;
		}

		void setStartTime(long startTime) {
			this.startTime = startTime;
		}

		public String getContent() {
			return content;
		}

		void setContent(String content) {
			this.content = content;
		}

		public long getEndTime() {
			return endTime;
		}

		void setEndTime(long endTime) {
			this.endTime = endTime;
		}

		@Override
		public int compareTo(LrcLine o) {
			return (int) (startTime - o.startTime);
		}

		@Override
		public String toString() {
			return "LrcLine [startTime=" + startTime + ", content=" + content
					+ ", endTime=" + endTime + "]";
		}

	}
	LrcLine testLine = new LrcLine();
	// 6暴露方法供调用—— 根据歌曲的播放时间，获取对应的歌词
	public LrcLine getLrcLine(long duration) {
		// 这个方法被频繁调用，必须高效
		// //floor的方法会获取比给定元素小的最大的元素
		// linesTs.floor(null);
		// //floor的方法会获取比给定元素大的最小的元素
		// linesTs.ceiling(null);

		// 判断duration 是否介于开始时间和结束时间，如果是的话，直接返回原先的对象
		if (lastFindLrcLine != null && lastFindLrcLine.between(duration)) {
			return lastFindLrcLine;
		}

		testLine.setStartTime(duration);
		LrcLine floor = linesTs.floor(testLine);
		lastFindLrcLine = floor;
		return floor;
	}

	TreeSet<LrcLine> linesTs;

	// 6暴露方法供调用—— 根据歌曲的播放时间，获取对应的歌词的序号
	public int getLrcLineIndex(long duration) {
		LrcLine lrcLine = getLrcLine(duration);
		return lines.indexOf(lrcLine);
	}

	// 6暴露方法供调用—— 获取全部歌词
	public List<LrcLine> getLrcLines() {
		return new ArrayList<LrcLine>(lines);
	}

	// 6暴露方法供调用 : 将此方法隐藏
	private Lrc() {
		lines = new ArrayList<LrcLine>();
	}

	// 6暴露方法供调用 : 将此方法隐藏
	private void addLine(LrcLine lrcLine) {
		lines.add(lrcLine);
	}

	// 5进行清理工作
	// 6暴露方法供调用 : 将此方法隐藏
	private void cleanUp() {
		// 1将单行歌词进行排序
		Collections.sort(lines);
		for (int i = 0; i < lines.size(); i++) {
			LrcLine line = lines.get(i);
			if (i + 1 < lines.size()) {
				// 2将下一行的歌词开始时间设置为当前行的结束时间，并减少1毫秒
				LrcLine nextLine = lines.get(i + 1);
				line.setEndTime(nextLine.startTime - 1);
			}
		}
		// 3找到最后一行歌词，如果没有内容的话，将其删除
		LrcLine lastLine = lines.get(lines.size() - 1);
		if (lastLine.getContent() == null
				|| lastLine.getContent().length() == 0) {
			lines.remove(lastLine);
		}
		linesTs = new TreeSet<LrcLine>();
		linesTs.addAll(lines);

	}

	// 6暴露方法供调用 : 将工具类转化为Lrc的内部类，并用工厂模式
	public static class Factory {
		public static Lrc create(File lrcFile) {
			// 0 创建一个歌词对象
			Lrc lrc = new Lrc();
			BufferedReader bfr = null;
			try {
				// 2将歌词文件一行一行进行处理
				bfr = getBufferedReader(lrcFile);
				// 3处理一行歌词
				String line = null;
				while ((line = bfr.readLine()) != null) {
					// 3处理一行歌词
					parseLine(lrc, line);
				}
				// 5进行清理工作
				lrc.cleanUp();
				return lrc;
			} catch (Exception e) {
				return null;
			} finally {
				close(bfr);
			}
		}

		public static Lrc create(String mp3Path) {
			return create(getLrcFile(mp3Path));
		}

		static Pattern linePatern = Pattern
				.compile("((?:\\[\\d+:\\d{1,2}\\.\\d{1,2}\\])+)(.*)");

		// 3处理一行歌词
		public static void parseLine(Lrc lrc, String line) {
			// 给定字符串line，与正在表达式linePatern匹配，返回的结果matcher是匹配的过程/结果
			Matcher matcher = linePatern.matcher(line);
			boolean matches = matcher.matches();
			if (matches) {
				String timeStamp = matcher.group(1);
				String content = matcher.group(2);
				// 往歌词对象Lrc中添加一行歌词LrcLine了
				// LrcLine lrcLine = null;
				//
				// lrc.addLine(lrcLine);
				// 复杂歌词可能有多个时间戳
				String[] stamps = splitMutiTimeStamp(timeStamp);
				for (int i = 0; i < stamps.length; i++) {
					// 根据时间戳算出毫秒值
					long startTime = parseMmssxx(stamps[i]);
					// System.out.println(startTime);
					// 构造LrcLine对象，添加进歌词Lrc中
					LrcLine lrcLine = new LrcLine();
					lrcLine.setStartTime(startTime);
					lrcLine.setContent(content);
					lrc.addLine(lrcLine);
				}
			}
		}
	}

	private static CodepageDetectorProxy detector = CodepageDetectorProxy
			.getInstance();
	static {
		detector.add(new ByteOrderMarkDetector());
		detector.add(JChardetFacade.getInstance());
	}

	public static Charset detectCharset(File f) throws MalformedURLException,
			IOException {
		return detector.detectCodepage(f.toURI().toURL());
	}

	public static BufferedReader getBufferedReader(File file)
			throws MalformedURLException, IOException {
		BufferedReader result;
		FileInputStream fis = new FileInputStream(file);
		Charset cs = detectCharset(file);
		InputStreamReader isr = new InputStreamReader(fis, cs);
		result = new BufferedReader(isr);
		return result;
	}

	public static boolean close(Closeable closeable) {
		if (closeable == null) {
			return false;
		}
		try {
			closeable.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String formatMMSS(long timeInMs) {
		// 250 000 = 250秒
		// 250 秒 = 4分钟(因为整数除法会向下取整)
		// 还有10秒

		// long second = timeInMs / 1000;
		// long minite = second / 60;
		// second = second% 60;
		// return String.format("%02d:%02d", minite ,second);

		return String.format("%02d:%02d", timeInMs / 1000 / 60,
				timeInMs / 1000 % 60);
	}

	// 提取多个时间戳
	static Pattern timeStampPatter = Pattern
			.compile("\\d+:\\d{1,2}\\.\\d{1,2}");

	public static String[] splitMutiTimeStamp(String timeStamp) {
		ArrayList<String> result = new ArrayList<String>();
		Matcher matcher = timeStampPatter.matcher(timeStamp);
		while (matcher.find()) {
			result.add(timeStamp.substring(matcher.start(), matcher.end()));
		}
		return result.toArray(new String[result.size()]);
	}

	// 处理mmssxx格式的时间戳为long值
	static Pattern mmssxxPatter = Pattern
			.compile("(\\d+):(\\d{1,2})\\.(\\d{1,2})");

	public static long parseMmssxx(String mmssxx) {
		Matcher matcher = mmssxxPatter.matcher(mmssxx);
		if (matcher.matches()) {
			int mm = Integer.parseInt(matcher.group(1));
			int ss = Integer.parseInt(matcher.group(2));
			int xx = Integer.parseInt(matcher.group(3));
			return mm * 60 * 1000 + ss * 1000 + xx * 10;
		}
		return -1;
	}

	static final String lrcHz1 = "txt";
	static final String lrcHz2 = "lrc";

	static Pattern commonAudio = Pattern
			.compile(
					".*\\.((aif)|(aiff)|(au)|(mp1)|(mp2)|(mp3)|(asx)|(m3u)|(pls)|(mlv)|(mpe)|(mpeg)|(mpg)|(mpv)|(mpa)|(ra/)|(rm)|(ram)|(snd)|(wav)|(voc)|(ins)|(cda))",
					Pattern.CASE_INSENSITIVE);

	// 1通过音乐路径获取歌词文件路径
	public static File getLrcFile(String musicPath) {
		// 截取最后一个.前面的字符串
		int lastDotPostion = musicPath.lastIndexOf('.');
		String lrcPart1 = musicPath.substring(0, lastDotPostion + 1);
		File txt = new File(lrcPart1 + lrcHz1);
		File lrc = new File(lrcPart1 + lrcHz2);

		if (lrc.exists()) {
			return lrc;
		}
		if (txt.exists()) {
			return txt;
		}
		return null;
	}
}
