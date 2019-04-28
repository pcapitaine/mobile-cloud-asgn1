package org.magnum.dataup;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/video")
public class VideoController {

//	private Set<Video> videoSet;
	private Map<Long, Video> videoMap;
	private static final AtomicLong currentId = new AtomicLong(0L);

	public VideoController() {
//		videoSet = new HashSet<Video>();
		videoMap = new HashMap<Long, Video>();
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public @ResponseBody List<Video> getVideoList() {
		List<Video> videos = videoMap.values().stream().collect(Collectors.toList());
		return videos;
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody Video addOrUpdateVideo(@RequestBody Video v, HttpServletResponse httpResponse) {
		// The video metadata is provided as an application/json request body. The JSON
		// should generate a valid instance of the Video class when deserialized by
		// Spring's default Jackson library.

		// The server should generate a unique identifier for the Video object and
		// assign it to the Video by calling its setId(...) method.

		// No video should have ID = 0. All IDs should be > 0.

		// The returned Video JSON should include this server-generated identifier so
		// that the client can refer to it when uploading the binary mpeg video content
		// for the Video.
		Video r = save(v);
		// Returns the JSON representation of the Video object that was stored along
		// with any updates to that object made by the server.
		// httpResponse.setStatus(HttpServletResponse.SC_OK);
		// httpResponse.set

		return r;
	}

	@RequestMapping(value = "/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus addVideoData(@RequestPart(value = "data") MultipartFile videoData,
			@PathVariable("id") long videoId, HttpServletResponse responseHeader) throws Exception {
		Video v = videoMap.get(videoId);
		if (v != null) {
			try {
				VideoFileManager.get().saveVideoData(v, videoData.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new VideoStatus(VideoState.READY);
		} else {
			responseHeader.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
	}

	@RequestMapping(value = "/{id}/data", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public @ResponseBody byte[] getVideoData(@PathVariable("id") long id, HttpServletResponse httpResponse) throws Exception {

		Video v = videoMap.get(id);
		if (v != null) {
			boolean hasVideoContent = VideoFileManager.get().hasVideoData(v);
			if(hasVideoContent)
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				VideoFileManager.get().copyVideoData(v, baos);
				return baos.toByteArray();	
			}			
		} else {
			httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		return null;
		
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		String base = "http://" + request.getServerName()
				+ ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
		return base;
	}

	public Video save(Video entity) {
		checkAndSetId(entity);
		checkAndSetDataUrl(entity);
		videoMap.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	private void checkAndSetDataUrl(Video entity) {
		if (entity.getDataUrl() == null) {
			entity.setDataUrl(getDataUrl(entity.getId()));
		}
	}

}
