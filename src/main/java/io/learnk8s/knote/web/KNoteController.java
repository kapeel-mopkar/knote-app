package io.learnk8s.knote.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.learnk8s.knote.config.KnoteProperties;
import io.learnk8s.knote.models.Note;
import io.learnk8s.knote.repos.NotesRepository;

@Controller
class KNoteController {
	
	private static final String FILE_NOT_FOUND_PNG = "file-not-found.png";

	private Path fileStorageLocation;

	@Autowired
	private NotesRepository notesRepository;

	@Autowired
	private KnoteProperties properties;
	
	@Value("${uploadDir:/tmp/uploads/}")
    private String uploadDir;
	
	@PostConstruct
	public void postInit() throws IOException {
		this.fileStorageLocation = Paths.get(uploadDir)
				.toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.fileStorageLocation);
		} catch (IOException ex) {
			throw new RuntimeException(
					"Could not create the directory where the uploaded files will be stored.", ex);
		}
	}

	@GetMapping("/")
	public String index(Model model) {
		getAllNotes(model);
		return "index";
	}

	private void getAllNotes(Model model) {
		List<Note> notes = notesRepository.findAll();
		Collections.reverse(notes);
		model.addAttribute("notes", notes);
	}

	@PostMapping("/note")
	public String saveNotes(@RequestParam("image") MultipartFile file, @RequestParam String description, @RequestParam String fileName,
			@RequestParam(required = false) String publish, @RequestParam(required = false) String upload, Model model)
			throws Exception {
		if (publish != null && publish.equals("Publish")) {
			saveNote(description, fileName, model);
			getAllNotes(model);
			return "redirect:/";
		}
		if (upload != null && upload.equals("Upload")) {
			if (file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
				uploadImage(file, description, model);
			}
			getAllNotes(model);
			return "index";
		}
		return "index";
	}

	private void uploadImage(MultipartFile file, String description, Model model) throws Exception {
		File uploadsDir = new File(properties.getUploadDir());
		if (!uploadsDir.exists()) {
			uploadsDir.mkdir();
		}
		String fileId = UUID.randomUUID().toString() + "." + file.getOriginalFilename().split("\\.")[1];
		file.transferTo(new File(properties.getUploadDir() + fileId));
		model.addAttribute("uploadedfilename",  fileId);
		model.addAttribute("description", description + " Attachment: " + file.getOriginalFilename());
	}

	private void saveNote(String description, String fileName, Model model) {
		if (description != null && !description.trim().isEmpty()) {
			notesRepository.save(new Note(null, description.trim(), fileName));
			// After publish you need to clean up the textarea
			model.addAttribute("description", "");
			model.addAttribute("uploadedfilename",  "file-not-found.png");
		}
	}
	
	@GetMapping("/downloadFile")
	public ResponseEntity<Resource> downloadFile(@RequestParam("fileName") String fileName,
			HttpServletRequest request) {
		Resource resource = null;
		if (fileName != null && !fileName.isEmpty()) {
			try {
				resource = loadFileAsResource(fileName); //documneStorageService.loadFileAsResource(fileName);
			} catch (FileNotFoundException e) {
				try {
					resource = loadFileAsResource(FILE_NOT_FOUND_PNG);
				} catch (FileNotFoundException e1) {
					return ResponseEntity.notFound().build();
				} 
			}

			// Try to determine file's content type
			String contentType = null;
			try {
				contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
			} catch (IOException ex) {
				//logger.info("Could not determine file type.");
			}
			// Fallback to the default content type if type could not be determined
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);

		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	private Resource loadFileAsResource(String fileName) throws FileNotFoundException {
		try {
			Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
			Resource resource = new UrlResource(filePath.toUri());
			if (resource.exists()) {
				return resource;
			} else {
				throw new FileNotFoundException("File not found " + fileName);
			}
		} catch (MalformedURLException ex) {
			throw new FileNotFoundException("File not found " + fileName);
		}
	}
}