package sg.nus.edu.secondleave.controllers;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import sg.nus.edu.secondleave.model.Comment;
import sg.nus.edu.secondleave.model.LeaveApplication;
import sg.nus.edu.secondleave.services.CommentService;
import sg.nus.edu.secondleave.services.LeaveApplicationService;
import sg.nus.edu.secondleave.util.LeaveEnum;

@Controller
@RequestMapping("/leaves")
public class LeaveApplicationController {
	
	@Autowired
	LeaveApplicationService leaveAppService;
	
	@Autowired
	CommentService commentService;

	@GetMapping("/view")
	public String viewLeaveApp(Model model) {
		List<LeaveApplication> leaveApps = leaveAppService.findLeaveApplications();
		model.addAttribute("leaves",leaveApps);
		return "LeaveApplicationView";
	}
	
	@GetMapping("/detail/{id}")
	public String detailLeaveApp(Model model,@PathVariable int id) {
		Comment comment = new Comment();
		Optional<LeaveApplication> leaveApp = leaveAppService.getLeaveApplication(id);
		List<LeaveApplication> leaveApps = leaveAppService.findLeaveApplications();
		model.addAttribute("leaves",leaveApps);
		model.addAttribute("leave",leaveApp.get());
		model.addAttribute("comment", comment);
		return "LeaveApplicationDetail";
	}
	
	@PostMapping("/decide/{id}")
	public String approveLeaveApp(@ModelAttribute("comment") @Valid Comment comment,@PathVariable int id) {
		Optional<LeaveApplication> leaveApp = leaveAppService.getLeaveApplication(id);
		comment.setLeave(leaveApp);
		commentService.saveComment(comment);
		
		if(comment.getDecision().equals("approved")) {
			leaveAppService.updateLeaveApplication(id,LeaveEnum.APPROVED.toString());
		} else {
			leaveAppService.updateLeaveApplication(id,LeaveEnum.REJECTED.toString());
		}
		return "LeaveApplicationView";
	}

}