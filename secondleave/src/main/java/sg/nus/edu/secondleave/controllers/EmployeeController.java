package sg.nus.edu.secondleave.controllers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import sg.nus.edu.secondleave.model.Employee;
import sg.nus.edu.secondleave.model.LeaveApplication;
import sg.nus.edu.secondleave.model.LeaveEntitlement;
import sg.nus.edu.secondleave.model.PageResult;
import sg.nus.edu.secondleave.model.Role;
import sg.nus.edu.secondleave.services.EmployeeServiceImpl;
import sg.nus.edu.secondleave.services.HolidayServiceImpl;
import sg.nus.edu.secondleave.services.LeaveApplicationServiceImpl;
import sg.nus.edu.secondleave.services.LeaveEntitlementServiceImpl;
import sg.nus.edu.secondleave.util.LeaveEnum;
import sg.nus.edu.secondleave.validators.LeaveFormValidator;

@Controller
public class EmployeeController {

// To view and save the leave apply form, Xin needs to autowire this Service
	@Autowired
	LeaveApplicationServiceImpl laService;
	@Autowired
	LeaveEntitlementServiceImpl leService;
	@Autowired
	HolidayServiceImpl hService;
	
	@Autowired
	EmployeeServiceImpl emService;
//	To validate the form staff posted, Xin need to autowire this Validator(it is in the validators package )
	@Autowired
	private LeaveFormValidator laValidator;

	@InitBinder("leave")
	private void initLeaveBinder(WebDataBinder binder) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		binder.addValidators(laValidator);

	}

	@RequestMapping(value = "/employee/history")
	public String staffHisotry(HttpSession session, Model model,
			@RequestParam(required = false, defaultValue = "1") int page,
					@RequestParam(required = false, value = "employeeId") Integer employeeId) {
		int size = 10;
		Employee emp = (Employee) session.getAttribute("validated");
		int isManager = 0;
		Set<Role> roles = emp.getRoles();
		for (Role role : roles) {
			if (role.getName().equalsIgnoreCase("manager")) {
				isManager = 1;
			}
		}
		model.addAttribute("isManager", isManager);
		if (emp != null) {
			List<LeaveApplication> leaveApplications = laService.findLeaveApplications();
			List<LeaveApplication> list = new ArrayList<>();
			if (employeeId != null) {
				emp.setEmployeeId(employeeId);
			}
			for (LeaveApplication leaveApplication : leaveApplications) {
				if (leaveApplication.getEmployee().getEmployeeId().equals(emp.getEmployeeId()) && leaveApplication.getStatus() != LeaveEnum.DELETED) {
					list.add(leaveApplication);
				}
			}
			if (list.size() > 0) {
				List<LeaveApplication> newList;
				if (list.size() > page * size) {
					newList = list.subList((page - 1) * size, page * size);
				} else {
					newList = list.subList((page - 1) * size, list.size());
				}
				int totalPage;
				if ((list.size() % 10) > 0) {
					totalPage = list.size() / 10 + 1;
				} else {
					totalPage = list.size() / 10;
				}
				model.addAttribute("totalPage", totalPage);
				model.addAttribute("lhistory", newList);
				model.addAttribute("total", list.size());
				model.addAttribute("page", page);
			}
			return "staffhistory";
		}

		return "forward:/home";
	}

	@RequestMapping(value = "/staff/historyTable")
	@ResponseBody
	public PageResult staffHistoryTable(HttpSession session, Model model,
										@RequestParam(required = false, defaultValue = "1") int page,
										@RequestParam(required = false, value = "employeeId") Integer employeeId) throws IOException {
		int size = 10;
		Employee emp = (Employee) session.getAttribute("validated");
		List<LeaveApplication> leaveApplications = laService.findLeaveApplications();
		List<LeaveApplication> list = new ArrayList<>();
		if (employeeId != null) {
			emp.setEmployeeId(employeeId);
		}
		for (LeaveApplication leaveApplication : leaveApplications) {
			if (leaveApplication.getEmployee().getEmployeeId().equals(emp.getEmployeeId()) && leaveApplication.getStatus() != LeaveEnum.DELETED) {
				list.add(leaveApplication);
			}
		}
		
		List<LeaveApplication> newList;
		if (list.size() > 0) {
			if (list.size() > page * size) {
				newList = list.subList((page - 1) * size, page * size);
			} else {
				newList = list.subList((page - 1) * size, list.size());
			}
			int totalPage;
			if ((list.size() % 10) > 0) {
				totalPage = list.size() / 10 + 1;
			} else {
				totalPage = list.size() / 10;
			}
			model.addAttribute("totalPage", totalPage);
			model.addAttribute("lhistory", newList);
			model.addAttribute("total", list.size());
			model.addAttribute("page", page);
			PageResult result = new PageResult();
			result.setPage(page);
			result.setTotal(list.size());
			result.setTotalPage(totalPage);
			result.setData(newList);
			return result;
		}
		return null;
	}


	@RequestMapping(value = "/employee/leave/edit/{leaveId}",method = RequestMethod.GET)
	public String update(@PathVariable Integer leaveId, Model model) {
		Optional<LeaveApplication> optional = laService.getLeaveApplication(leaveId);
		if (optional.isPresent()) {
			LeaveApplication leaveApplication = optional.get();
			Employee employee = emService.findEmpById(leaveApplication.getEmployee().getEmployeeId());
			List<String> leaveTypeList = laService.findAllLeaveType();
			// ?????????key??????????????????????????? th:action="newLeave"????????????
			model.addAttribute("leaveTypeList", leaveTypeList);
			model.addAttribute("employee", employee);
			model.addAttribute("leaveApplication", leaveApplication);
			return "updateRecord";
		} else {
			return "error";
		}
	}

	//????????????
	@RequestMapping(value = "/employee/leave/edit/{employeeId}",method = RequestMethod.POST)
	public String updateAndSave(LeaveApplication leaveApplication, BindingResult bindingResult) {
		leaveApplication.setStatus(LeaveEnum.UPDATED);
		laService.updateLeaveApplication(leaveApplication);
		return "redirect:/employee/history";
	}
	//????????????
	@RequestMapping(value = "employee/leave/delete/{employeeId}")
	public String deleteLeave(@PathVariable Integer employeeId,Model model,@ModelAttribute LeaveApplication LA) {
		LeaveApplication la = laService.findLeaveApplicationsByEmployeeId(employeeId).get(0);
		return null;


	}

	@GetMapping("/employee/deleteLeave")
	@ResponseBody
	public String deleteLeave(int id) {
		laService.updateLeaveApplication(id, LeaveEnum.DELETED.name());
		return "0";
	}

	// Made By Xin to show the leave apply form and save the form
	@RequestMapping("/employee/applyleave")
	public String viewLeaveForm(Model model, @ModelAttribute("User") Employee emp) {
		// ????????????????????????????????????????????????whitelabel??????????????????????????????????????????leaveapplication????????????????????????????????????
		LeaveApplication la = new LeaveApplication();
		
		// ????????????laService??????????????????enum leavetype ??? List<String>???????????????????????????????????????????????????
		List<String> laType = laService.findAllLeaveType();
		
		// ?????????key??????????????????????????? th:action="newLeave"????????????
		model.addAttribute("newLeave", la);
		model.addAttribute("leavetype", laType);
		
		return "leaveform-apply";
	}

	@RequestMapping(value = "/employee/saveleave", method = RequestMethod.POST)
	public ModelAndView saveLeaveForm(
			@ModelAttribute("newLeave") @Valid LeaveApplication leave,
			BindingResult bindingResult, 
			HttpSession session) 
	{				
		// ????????????session?????????????????????employee??????
		Employee emp = (Employee) session.getAttribute("validated");
		leave.setEmployee(emp);
		ModelAndView mvSuccess = new ModelAndView();
		ModelAndView mvFail = new ModelAndView();
		if (bindingResult.hasErrors()) 
		{
			// back to view with empty form and all leave type
			//mvFail.addObject("newLeave", new LeaveApplication());
			mvFail.addObject("leavetype", laService.findAllLeaveType());
			mvFail.setViewName("leaveform-apply");
			return mvFail;
		}
		// if the binding success
		LeaveEntitlement leaveEntitlement = leService.findTopByEmployeeAndType(emp, leave.getType());
		// ????????????????????????????????????
		//double balance = leaveEntitlement.getEntitlement();
		if(!hService.isWorkingDay(leave))
		{
			String msg = "Pleace check your calendar to make sure the From and To date is not SG holiday or weekend!!!";
			mvFail.addObject("errormsg", msg);
			mvFail.addObject("leavetype", laService.findAllLeaveType());
			mvFail.setViewName("leaveform-apply");
			return mvFail;
		}
		if(!hService.isBalanceEnough(leave))
		{
			List<LeaveEntitlement> entitlements = leService.findByEmployee(emp);
			
			String errorMsg = 
					"Submit Fail! Your "+leaveEntitlement.getType()+
					" Balance is Not Enought: Only "+ 
					leaveEntitlement.getEntitlement() +" Day left.";
			mvFail.addObject("entitlements", entitlements);
			mvFail.addObject("balanceerror", errorMsg);
			mvFail.setViewName("balance-not-enough");
			return mvFail;
		}
//		Period p = Period.between(start, end);
//		int res = p.getDays();
		
		int leaveDays = hService.findLeaveDaysWithoutHoliday(leave.getFromDate(), leave.getToDate());
				
		String message = "New leaveapplication " + leave.getType() + " was successfully created.";
		System.out.println(message);



		// ???????????????????????????APPLIED
		leave.setStatus(LeaveEnum.APPLIED);
		mvSuccess.setViewName("redirect:history");
		// ???????????????????????????????????????
		laService.saveLeaveApplication(leave);
		return mvSuccess;
	}
	
	@GetMapping(value = "/employee/export")
	public void exportToCSV(HttpServletResponse response)throws IOException{
		response.setContentType("text/csv");
		DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
		String currentDateTime = dateFormatter.format(new Date());
		String fileName = "records"+currentDateTime+".csv";
		
		String headerKey = "Content-Disposition";
		String headervalue = "attachment;filename="+fileName;
		
		response.setHeader(headerKey, headervalue);
		
		List<LeaveApplication> listLeaveApps = laService.listAll();
		
		ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(),CsvPreference.STANDARD_PREFERENCE);
		
		String[] csvHeader = {"Id","Type","From Date","To Date","Status","Reason","Work Dissemination","Contact Details"};
		String[] nameMapping = {"leaveAppId","type","fromDate","toDate","status","reason","workDissemination","contactDetails"};
		
		csvWriter.writeHeader(csvHeader);
		
		for(LeaveApplication la: listLeaveApps) {
			csvWriter.write(la, nameMapping);
		}
		
		csvWriter.close();
	}
}
