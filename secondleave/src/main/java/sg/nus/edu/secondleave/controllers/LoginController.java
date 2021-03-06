package sg.nus.edu.secondleave.controllers;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import sg.nus.edu.secondleave.model.Employee;
import sg.nus.edu.secondleave.services.EmployeeService;

@Controller
public class LoginController {
	
	@Autowired
	private EmployeeService eService;
	
	
	@RequestMapping(value="/")
	public String standard(Model model, HttpSession session) {
		if(session.getAttribute("validated") != null || session.getAttribute("admvalidated") !=null) {
			return "defaultpage";
		}
		model.addAttribute("user", new User());
		return "login";
	}
	
	@RequestMapping(value="/home")
	public String home(Model model, HttpSession session) {
		if(session.getAttribute("validated") != null || session.getAttribute("admvalidated") !=null) {
			return "defaultpage";
		}
		model.addAttribute("user", new User());
		return "login";
	}
	
	@RequestMapping(value="/admin")
	public String admin(Model model, HttpSession session) {
		if(session.getAttribute("validated") != null || session.getAttribute("admvalidated") !=null) {
			return "defaultpage";
		}
		model.addAttribute("user", new User());
		return "adminlogin";
	}
	
	@RequestMapping(value="/home/validate")
	public String loginValidate(@ModelAttribute("user") @Valid User user
			,BindingResult bindingResult, Model model, HttpSession session) {

		 Employee emp = eService.authenticate(user.getUsername(),user.getPassword());
		if(bindingResult.hasErrors()) {
			return "login";
		} else {
			if(emp == null) {
				return "loginfail";
			} 
					
			session.setAttribute("validated", emp);
			return "forward:/employee/history";
		}
	}
	
	@RequestMapping(value="/admin/validate")
	public String adminValidate(@ModelAttribute("user") @Valid User user
			,BindingResult bindingResult, Model model, HttpSession session) {

		if(bindingResult.hasErrors()) {
			return "adminlogin";
		} else {
			Employee emp = eService.authenticate(user.getUsername(),user.getPassword());
			
			if(emp == null || !emp.getRoles().stream()
					.anyMatch(x->x.getName().equals("Administrator"))) {
				return "adminloginfail";
			} 
					
			session.setAttribute("admvalidated", emp);
			return "forward:/admin/history";
		}
	}
	
	@RequestMapping(value = "/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/home";

	}
}
