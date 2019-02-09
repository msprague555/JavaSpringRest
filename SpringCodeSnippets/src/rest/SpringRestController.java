package rest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.att.cemg.frontdoor.entity.Developer;
import com.att.cemg.frontdoor.entity.Note;
import com.att.cemg.frontdoor.entity.Task;
import com.att.cemg.frontdoor.entity.TaskStatus;
import com.att.cemg.frontdoor.entity.TaskType;
import com.att.cemg.frontdoor.repository.DeveloperRepository;
import com.att.cemg.frontdoor.repository.NoteRepository;
import com.att.cemg.frontdoor.repository.TaskRepository;
import com.att.cemg.frontdoor.repository.TaskStatusRepository;
import com.att.cemg.frontdoor.repository.TaskTypeRepository;
import com.att.cemg.frontdoor.service.EmailService;


@RestController
public class TaskRestController {
	
	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private NoteRepository noteRepository;
	
	@Autowired
	private TaskTypeRepository taskTypeRepository;
	
	@Autowired
	private TaskStatusRepository taskStatusRepository;
	
	@Autowired
	@Qualifier("FrontDoorJdbcTemplate")
	JdbcTemplate frontDoorTemplate;
	
	@Autowired
	private DeveloperRepository developerRepository;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	EmailService emailService;
	
	@RequestMapping(value="/add_note",method = {RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_WRITE')";)
	public Integer addNote(Integer projectNum, String attuid, String note, Date insDate, Integer type,
			Boolean gbrFlag) {
		Integer returnInt = 1;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String insertBy = auth.getName();
		Note newNote = new Note(projectNum, insertBy, note, insDate, type, gbrFlag);
		try {
			noteRepository.save(newNote);
		}
		catch (DataAccessException e1) { 
			returnInt = -1;
		}
		return returnInt;
	}
	
	@RequestMapping(value="/add_new_task",method = {RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_WRITE')";)
	public Integer addTask(HttpServletRequest request, Integer projectNum, String owner, String desc, Date ecd, Integer type
			) {
		Integer returnInt = 1;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String insertBy = auth.getName();
		Date d = new Date();
		desc = desc.replace("\"", "");
		desc = desc.replace("\'", "");
		Task newTask = new Task(projectNum, owner, desc, ecd, ecd, insertBy, d, 0, type, 10, false);
		//SAVE TASK AND RETURN OBJECT WE CAN USE TO GET THE NEW TASK ID
		Task resp = null;
		try {
			resp = taskRepository.save(newTask);
			
			}
			catch(DataAccessException e) {
				returnInt = -1;//task failed to save
			}
		if(resp != null) {
				//BEGIN NOTE GENERATION PROCESS
				String note = "Task Added: " + resp.getSerno() 
				+ ": " + resp.getDescription();
				Boolean i = false;
				Note addNote = new Note(resp.getReqSerno(), insertBy, note, d, 1, i);
				try {
				noteRepository.save(addNote);
				}
				catch(DataAccessException e) {
					returnInt = -2;//note failed to save
				}
				//END NOTE GENERATION PROCESS
				emailService.mail(request, type, desc, "", owner, "TASK_ADD", projectNum, ecd);
			}
		return returnInt; 
	}

	@RequestMapping(value="/delete_task",method = {RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_WRITE')";)
	public long deleteTask(HttpServletRequest request, Integer taskId) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String deletedBy = auth.getName();
		Task t = taskRepository.findOne(taskId);
		String owner = t.getOwner();
		Integer projectNum = t.getReqSerno();
		String desc = t.getDescription();
		Date ecd = t.getEcd();
		Long resp = taskRepository.deleteBySerno(taskId);
		if(resp == 1) {
			String note = "Task Deleted:" + t.getSerno() + ": " + t.getDescription();
			Date d = new Date();
			Boolean i = false;
			//BEGIN NOTE GENERATION PROCESS
			Note deleteNote = new Note(t.getReqSerno(), deletedBy, note, d, 1, i);
			noteRepository.save(deleteNote);
			//END NOTE GENERATION PROCESS
			emailService.mail(request, t.getType(), desc, "", owner, "TASK_DELETE", projectNum, ecd);
		}
		return resp;
	}
	
	@RequestMapping(value="/dev_autocomplete",method = {RequestMethod.GET})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_READ')";)
	public Iterable<Developer> devAutocomplete(HttpServletRequest request, String partialSearch) {
		Iterable<Developer> i = developerRepository.findAllWithPartialSearch(partialSearch);
		
		return i;
	}
	
	@RequestMapping(value="/edit_task",method = {RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_WRITE')";)
	public Integer editTask(HttpServletRequest request, Integer taskId, String owner, String desc,
	Date ecd, Integer type, Date acd, Integer pctComplete, Integer status, String ecdReason) {
		Integer returnInt = 1;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		desc = desc.replace("\"", "");
		desc = desc.replace("\'", "");
		String editedBy = auth.getName();
		Task originalTask = taskRepository.findOne(taskId);
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String ecdString = "";
		if(ecd != null) {
			ecdString = df.format(ecd);
		}
		String supervisor = originalTask.getWebphone().getSupervisorId();
		String ttDesc = "";
		String tsDesc = "";
		Date baselineEcd = originalTask.getBaselineEcd();
		Boolean baselineFlag = originalTask.getBaseLineFlag();
		Boolean rebase = originalTask.getBaseLineFlag();
		String originalOwner, originalDesc;
		Date originalEcd;
		Integer originalType, originalPctComplete, originalStatus;
		originalOwner = originalTask.getOwner();
		originalDesc = originalTask.getDescription();
		originalEcd = originalTask.getEcd();
		String originalEcdFormatted = "";
		originalEcdFormatted = df.format(originalEcd);

		originalType = originalTask.getType();
		originalPctComplete = originalTask.getPctComplete();
		originalStatus = originalTask.getStatus();
		ArrayList<String> noteEdits = new ArrayList<String>();
		if(ecd != null) {
			if(ecd.after(baselineEcd)) {
				rebase = true;
			}
			if(baselineFlag) {
				rebase = false;
			}
			if(editedBy.equalsIgnoreCase(supervisor)) {
				rebase = false;
				if(!ecd.equals(originalTask.getEcd())) {
					originalTask.setBaseLineFlag(false);
					originalTask.setBaselineEcd(ecd);
					originalTask.setEcd(ecd);
				}
			}
			if(acd != null) {
				rebase = false;
			}
			if(originalEcd.equals(ecd)) {
				rebase = false;
			}
		}
		if(owner != null) {
			originalTask.setOwner(owner);
		}
		if(desc != null) {
			originalTask.setDescription(desc);
		}
		if(ecd != null && ecd.compareTo(originalEcd) != 0) {
			originalTask.setEcd(ecd);
		}
		if(type != null) {
			originalTask.setType(type);
			TaskType tt = taskTypeRepository.findOne(type);
			ttDesc = tt.getDescription();
		}
		if(acd != null) {
			
		}
		if(pctComplete != null) {
			originalTask.setPctComplete(pctComplete);
		}
		if(status != null) {
			originalTask.setStatus(status);
			TaskStatus ts = taskStatusRepository.findOne(status);
			tsDesc = ts.getDescription();
		}
		if(rebase) {
			originalTask.setBaseLineFlag(rebase);
		}
		if(acd != null) {
			originalTask.setAcd(acd);
			originalTask.setPctComplete(100);
			originalTask.setStatus(100);
			TaskStatus ts = taskStatusRepository.findOne(100);
			tsDesc = ts.getDescription();
			noteEdits.add("Task Completed");
		}

		
		 //Task editedTask = taskRepository.findOne(taskId);
		//TaskStatus editedTaskStatus = editedTask.getTaskStatus();
		//BEGIN NOTE GENERATION PROCESS
		if(!originalOwner.equalsIgnoreCase(originalTask.getOwner())) {
			noteEdits.add("Owner changed from " + originalOwner + " to " + owner);
		}
		if(!originalDesc.equalsIgnoreCase(originalTask.getDescription())) {
			noteEdits.add("Description changed from " + originalDesc + " to " + desc);
		}
		if(!originalEcd.equals(originalTask.getEcd())) {
			noteEdits.add("Ecd changed from " + originalEcdFormatted + " to " + ecdString);
		}
		if(ecdReason != null && !ecdReason.equalsIgnoreCase("")) {
			noteEdits.add("Reason: " + ecdReason);
		}
		if(originalType != originalTask.getType()) {
			noteEdits.add("Type changed to " + ttDesc);
		}
		if(originalPctComplete != originalTask.getPctComplete()) {
			noteEdits.add("Pct Complete changed to " + originalTask.getPctComplete());
		}
		if(originalStatus != originalTask.getStatus()) {
			noteEdits.add("Status changed to " + tsDesc);
		}

		Date d = new Date();
		Boolean b = false;
		String note = "Task Edited: " + originalTask.getSerno() + ": (" + originalTask.getDescription() + "): ";
		for(int i = 0; i<noteEdits.size(); i++){
			if(i == 0) {
				note += noteEdits.get(i);
			}
			else {
				note += ", " + noteEdits.get(i);
			}
		}
			Task editedTask = null;
		try {
			editedTask = taskRepository.save(originalTask);
			}
			catch(DataAccessException e) {
				returnInt = -1;//task failed to save
			}
		if(editedTask != null) {
			Note editNote = new Note(originalTask.getReqSerno(), editedBy, note, d, 1, b);
			try {
				editNote = noteRepository.save(editNote);
				}
				catch(DataAccessException e) {
					returnInt = -2;//note failed to save
				}
		
			//END NOTE GENERATION PROCESS
			Integer projectNum = 0;
			projectNum = editedTask.getReqSerno();
			//SEND EMAIL FOR TASK EDIT
			String mailType = "TASK_EDIT";
			if(acd != null) {
				mailType = "TASK_COMPLETE";
			}
			emailService.mail(request, type, desc, "", owner, mailType, projectNum, ecd);
			//IF THE EDIT FLAGGED A REQUIRED REBASE, SEND MANAGER EMAIL TO NOTIFY
			if(rebase) {
				emailService.mail(request, type, desc, ecdReason, owner, "TASK_REBASE_REQUIRED", projectNum, ecd);
			}
		}
		return returnInt;
	}
	
	@RequestMapping(value="/tasks_for_userid",method = {RequestMethod.GET})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_READ')";)
	public Set<Task> getTasksForUserid(String userid) {
		Set<Task> t = taskRepository.findByOwnerOrderByRequestGbrPriority(userid);
		return t;
	}
	
	@RequestMapping(value="/tasks_for_am",method = {RequestMethod.GET})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_READ')";)
	public Set<Task> getTasksForAm(String am) {
		Set<Task> t = taskRepository.findByWebphoneSupervisorIdOrderByRequestGbrPriority(am);
		return t;
	}
	
	@RequestMapping(value="/tasks_for_project",method = {RequestMethod.GET})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_READ')";)
	public Set<Task> getTasksForProject(Integer projectSerno) {
		Set<Task> t = taskRepository.findByReqSerno(projectSerno);
		return t;
	}
	
	@RequestMapping(value="/task_status",method = {RequestMethod.GET,RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_READ')";)
	public Iterable<TaskStatus> getTaskStatus() {
		
		return taskStatusRepository.findBySernoNotLike(100);
	}
	
	@RequestMapping(value="/task_type",method = {RequestMethod.GET,RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_READ')";)
	public Iterable<TaskType> getTaskType() {
		return taskTypeRepository.findAll();
	}
	
	@RequestMapping(value="/rebase_task",method = {RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_WRITE')";)
	public Integer rebaseTask(HttpServletRequest request, Integer taskId, Date baselineECD, String baseNotes) {
		Integer returnInt = 1;
		Task originalTask = taskRepository.findOne(taskId);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String rebaseBy = auth.getName();
		Boolean didRebase = false;
		baseNotes = baseNotes.replace("\"", "");
		baseNotes = baseNotes.replace("\'", "");
		if(originalTask.getEcd().equals(baselineECD)) {
			didRebase = true;
		}
		originalTask.setBaselineEcd(baselineECD);
		originalTask.setBaseLineFlag(false);
		Task rebasedTask = null;
		try {
		rebasedTask = taskRepository.save(originalTask);
		}
		catch(DataAccessException e) {
			returnInt = -1;//task failed to save
		}
		if(rebasedTask != null) {
			//BEGIN NOTE GENERATION PROCESS
			Date d = new Date();
			Boolean i = false;
			String note;
			if(didRebase) {
				note = "Task Rebaselined: " + rebasedTask.getSerno() + ": (" + rebasedTask.getDescription() + "): " + baseNotes;
			}
			else {
				note = "Task Rebaseline Rejected: " + rebasedTask.getSerno() + ": (" + rebasedTask.getDescription() + "): " + baseNotes;
			}
			Note rebasedNote = new Note(rebasedTask.getReqSerno(), rebaseBy, note, d, 5, i);
			try {
				noteRepository.save(rebasedNote);
			}
			catch(DataAccessException e) {
				returnInt = -2;//note failed to save
			}
			//END NOTE GENERATION PROCESS
			Integer projectNum = 0;
			String desc, owner = "";
			owner = rebasedTask.getOwner();
			Date ecd = rebasedTask.getEcd();
			desc = rebasedTask.getDescription();
			projectNum = rebasedTask.getReqSerno();

			String emailType = "";
			if(didRebase) {
				emailType = "TASK_REBASE";
			}
			else {
				emailType = "TASK_REBASE_REJECTED";
			}
			emailService.mail(request, rebasedTask.getType(), desc, baseNotes, owner, emailType, projectNum, ecd);

		}
		return returnInt;
	}
	
	@RequestMapping(value="/reopen_task",method = {RequestMethod.POST})
	@PreAuthorize("hasAnyAuthority('ACCESS_LEVEL_WRITE')";)
	public Integer reopenTask(HttpServletRequest request, Integer taskId) {
		Integer returnInt = 1;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String reopenedBy = auth.getName();
		Task originalTask = taskRepository.findOne(taskId);
		originalTask.setAcd(null);
		originalTask.setStatus(10);
		Task reopenedTask = null;
		try {
			reopenedTask = taskRepository.save(originalTask);
		}
		catch(DataAccessException e) {
			returnInt = -1;//task failed to save
		}
		if(reopenedTask != null) {
			//BEGIN NOTE GENERATION PROCESS
			Date d = new Date();
			Boolean i = false;
			String note = "Task Re-Opened: " + reopenedTask.getSerno() + ": (" + reopenedTask.getDescription() + ")";
			Note rebasedNote = new Note(reopenedTask.getReqSerno(), reopenedBy, note, d, 1, i);
			try {
				noteRepository.save(rebasedNote);
			}
			catch(DataAccessException e) {
				returnInt = -2;//note failed to save
			}
			//END NOTE GENERATION PROCESS
			Integer projectNum = 0;
			String desc, owner = "";
			owner = reopenedTask.getOwner();
			Date ecd = reopenedTask.getEcd();
			desc = reopenedTask.getDescription();
			projectNum = reopenedTask.getReqSerno();
			emailService.mail(request, reopenedTask.getType(), desc, String.valueOf(reopenedTask.getEcd()), owner, "TASK_REOPEN", projectNum, ecd);
		}
		return returnInt;
	}
	
}

