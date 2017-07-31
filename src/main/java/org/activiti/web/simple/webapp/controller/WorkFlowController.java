package org.activiti.web.simple.webapp.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.web.simple.webapp.model.Leave;
import org.activiti.web.simple.webapp.service.LeaveWorkFlowService;
import org.activiti.web.simple.webapp.service.WorkflowTraceService;
import org.activiti.web.simple.webapp.util.Variables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(value = "/workflow")
public class WorkFlowController {

	@Autowired
	private WorkflowTraceService traceService;

	@SuppressWarnings("unused")
	@Resource(name = "identityService")
	private IdentityService identityService;

	@Resource(name = "runtimeService")
	private RuntimeService runtimeService;

	@SuppressWarnings("unused")
	@Resource(name = "historyService")
	private HistoryService historyService;

	@SuppressWarnings("unused")
	@Resource(name = "taskService")
	private TaskService taskService;

	@SuppressWarnings("unused")
	@Resource(name = "managementService")
	private ManagementService managementService;

	@SuppressWarnings("unused")
	@Resource(name = "formService")
	private FormService formService;

	@Resource(name = "repositoryService")
	private RepositoryService repositoryService;

	@Resource(name = "leaveWorkFlowServiceImpl")
	private LeaveWorkFlowService leaveWorkFlowService;

	@RequestMapping("/toupload")
	public String toupload() {
		return "workflow/upload";
	}

	/**
	 * 部署流程定义文件(Spring MVC文件上传)
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/deploy", method = RequestMethod.POST)
	public String deploy(@RequestParam("username") String username, @RequestParam("file") MultipartFile file,
			HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes)
			throws Exception {
		System.out.println(username);
		if (!file.isEmpty()) {
			// 获取文件字节数组
			byte[] bytes = file.getBytes();
			// 获取文件保存路径
			String realPath = request.getSession().getServletContext().getRealPath("/upload");
			File out = new File(realPath, file.getOriginalFilename());
			// 将文件写到指定目录下
			FileUtils.writeByteArrayToFile(out, bytes);

			if (FilenameUtils.getExtension(file.getOriginalFilename()).equals("zip")
					|| FilenameUtils.getExtension(file.getOriginalFilename()).equals("bar")) {

				ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
				// 部署流程定义文件
				repositoryService.createDeployment().addZipInputStream(zipInputStream).deploy();

			} else {
				redirectAttributes.addFlashAttribute("message", "请上传zip或bar格式的文件!");
				return "redirect:/workflow/toupload";
			}
			redirectAttributes.addFlashAttribute("message", "文件上传成功!保存在:" + out.getPath());
		}
		return "redirect:/workflow/processlist";
	}

	@RequestMapping(value = "/processlist", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView processlist(HttpServletRequest request, HttpServletResponse response) {

		ModelAndView modelAndView = new ModelAndView("workflow/processlist");

		/*
		 * 保存两个对象，一个是ProcessDefinition（流程定义），一个是Deployment（流程部署）
		 */
		List<Object[]> objects = new ArrayList<Object[]>();

		List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
		for (ProcessDefinition processDefinition : list) {
			String deploymentId = processDefinition.getDeploymentId();
			Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
			objects.add(new Object[] { processDefinition, deployment });
		}
		modelAndView.addObject("objects", objects);
		return modelAndView;
	}

	/**
	 * 根据流程部署Id和资源名称加载流程资源
	 * 
	 * @param deploymentId
	 * @param resourceName
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/loadResourceByDeployment", method = { RequestMethod.GET, RequestMethod.POST })
	public void loadResourceByDeployment(@RequestParam("deploymentId") String deploymentId,
			@RequestParam("resourceName") String resourceName, HttpServletRequest request,
			HttpServletResponse response) {

		InputStream resourceAsStream = repositoryService.getResourceAsStream(deploymentId, resourceName);
		try {
			byte[] byteArray = IOUtils.toByteArray(resourceAsStream);
			ServletOutputStream servletOutputStream = response.getOutputStream();
			servletOutputStream.write(byteArray, 0, byteArray.length);
			servletOutputStream.flush();
			servletOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据流程部署Id级联删除已部署的流程
	 * 
	 * @param deploymentId
	 * @param request
	 * @param response
	 * @return 跳转到已部署的流程列表
	 */
	@RequestMapping(value = "/deleteDeploymentById/{deploymentId}", method = { RequestMethod.GET })
	public String deleteDeploymentById(@PathVariable("deploymentId") String deploymentId, HttpServletRequest request,
			HttpServletResponse response, RedirectAttributes redirectAttributes) {
		try {
			repositoryService.deleteDeployment(deploymentId, true);
			redirectAttributes.addFlashAttribute("message", "已部署的流程" + deploymentId + "已成功删除!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("message", "已部署的流程" + deploymentId + "删除失败!");
		}
		return "redirect:/workflow/processlist";
	}

	/**
	 * 根据流程实例Id和资源类型加载流程资源
	 * 
	 * @param processInstanceId
	 *            流程实例id
	 * @param resourceType
	 *            流程资源类型
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/loadResourceByProcessInstance", method = { RequestMethod.GET, RequestMethod.POST })
	public void loadResourceByProcessInstance(@RequestParam("processInstanceId") String processInstanceId,
			@RequestParam("resourceType") String resourceType, HttpServletRequest request,
			HttpServletResponse response) {
		// 根据流程实例id查询流程实例
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		// 根据流程定义id查询流程定义
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

		String resourceName = "";
		if (resourceType.equals("xml")) {
			// 获取流程定义资源名称
			resourceName = processDefinition.getResourceName();
		} else if (resourceType.equals("image")) {
			// 获取流程图资源名称
			resourceName = processDefinition.getDiagramResourceName();
		}
		// 打开流程资源流
		InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
				resourceName);
		// 输出到浏览器
		try {
			byte[] byteArray = IOUtils.toByteArray(resourceAsStream);
			ServletOutputStream servletOutputStream = response.getOutputStream();
			servletOutputStream.write(byteArray, 0, byteArray.length);
			servletOutputStream.flush();
			servletOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 请求转发到查看流程图页面
	 * 
	 * @param taskId
	 * @return
	 */
	@RequestMapping(value = "/view/{executionId}/page/{processInstanceId}", method = { RequestMethod.GET,
			RequestMethod.POST })
	public ModelAndView viewImage(@PathVariable("executionId") String executionId,
			@PathVariable("processInstanceId") String processInstanceId) {
		ModelAndView modelAndView = new ModelAndView("workflow/view");
		modelAndView.addObject("executionId", executionId);
		modelAndView.addObject("processInstanceId", processInstanceId);
		return modelAndView;
	}

	/**
	 * 根据流程实例id查询流程图(跟踪流程图)
	 * 
	 * @param processInstanceId
	 *            流程实例id
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/view/{processInstanceId}", method = { RequestMethod.GET, RequestMethod.POST })
	public void viewProcessImageView(@PathVariable("processInstanceId") String processInstanceId,
			HttpServletRequest request, HttpServletResponse response) {
		InputStream resourceAsStream = null;
		try {

			// 根据流程实例id查询流程实例
			ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();

			// 根据流程定义id查询流程定义
			ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
					.processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

			String resourceName = processDefinition.getDiagramResourceName();

			// 打开流程资源流
			resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);

			runtimeService.getActiveActivityIds(processInstance.getId());

			// 输出到浏览器
			byte[] byteArray = IOUtils.toByteArray(resourceAsStream);
			ServletOutputStream servletOutputStream = response.getOutputStream();
			servletOutputStream.write(byteArray, 0, byteArray.length);
			servletOutputStream.flush();
			servletOutputStream.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 输出跟踪流程信息
	 * 
	 * @param processInstanceId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/process/{executionId}/trace/{processInstanceId}", produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public @ResponseBody Map<String, Object> traceProcess(@PathVariable("executionId") String executionId,
			@PathVariable("processInstanceId") String processInstanceId) throws Exception {

		// 根据executionId查询当前执行的节点
		ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery()
				.processInstanceId(processInstanceId).executionId(executionId).singleResult();

		// 获取当前节点的activityId
		String activityId = execution.getActivityId();

		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();

		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
				.getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

		List<ActivityImpl> activities = processDefinitionEntity.getActivities();

		Map<String, Object> activityImageInfo = new HashMap<String, Object>();

		for (ActivityImpl activityImpl : activities) {
			String id = activityImpl.getId();
			// 判断是否是当前节点
			if (id.equals(activityId)) {
				activityImageInfo.put("x", activityImpl.getX());
				activityImageInfo.put("y", activityImpl.getY());
				activityImageInfo.put("width", activityImpl.getWidth());
				activityImageInfo.put("height", activityImpl.getHeight());
				break;// 跳出循环
			}
		}
		return activityImageInfo;
	}

	/**
	 * 输出跟踪流程信息
	 * 
	 * @param processInstanceId
	 *            流程实例id
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/process/{pid}/trace")
	@ResponseBody
	public List<Map<String, Object>> traceProcess(@PathVariable("pid") String processInstanceId) throws Exception {
		List<Map<String, Object>> activityInfos = traceService.traceProcess(processInstanceId);
		return activityInfos;
	}

	// 以下方法为提供rest接口,执行流程的审批过程
	/***
	 * 流程启动方法
	 * 
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException 
	 */
	@ResponseBody
	@RequestMapping(value = "/start", method = { RequestMethod.POST })
	public JSONObject startRestWorkFlow(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		//读取body中的参数
		Map<String, Object> bodyParams = new HashMap<String, Object>();
		StringBuffer str = getRequestContent(request);
		JSONObject obj = JSONObject.fromObject(str.toString());
		bodyParams.put("userId", obj.getString("userId"));
		String deptUserStr = obj.getString("deptUserId");
		String deptUser = deptUserStr.replaceAll("\"", "");
		//System.out.println(deptUser);
		bodyParams.put("deptUserId", deptUser.substring(1, deptUser.length()-1));
		bodyParams.put("businessType", obj.getString("businessType"));
		bodyParams.put("businessKey", obj.getString("businessKey"));
		
		Map<String, Object> variables = new HashMap<String, Object>();
		// 设定邮件发送人和邮件收件人
		variables.put("from", "184675420@qq.com");
		Map<String, Object> tempMap = new HashMap<String, Object>();
		// 流程发起人
		String userId = (String)bodyParams.get("userId");
		// 流程待办人ID
		String deptUserId = (String)bodyParams.get("deptUserId");
		variables.put("deptUserId", deptUserId);
		variables.put("userId", userId);
		System.out.println("userId===" + userId + "==deptUserId===" + deptUserId);
		// 与业务绑定(将请假实例的id与流程实例绑定)
		String businessKey = (String)bodyParams.get("businessKey");
		// 用来设置启动流程的人员ID，引擎会自动把用户ID保存到activiti:initiator中
		identityService.setAuthenticatedUserId(userId);
		// 业务类型
		String businessType = (String)bodyParams.get("businessType");
		Map<String, Object> msgMap = new HashMap<String, Object>();
		try {
			ProcessInstance processInstance = leaveWorkFlowService.
					startWorkflow(businessType, businessKey, variables);
			msgMap.put("code", 0);
			msgMap.put("message", "流程启动成功");
			Map<String, Object> dataMap = new HashMap<String, Object>();
			dataMap.put("processId", processInstance.getId());
			dataMap.put("activityId", processInstance.getActivityId());
			//dataMap.put("activityName", processInstance.getName());
			tempMap.put("data", dataMap);
		} catch (ActivitiException e) {
			if (e.getMessage().indexOf("no processes deployed with key") != -1) {
				msgMap.put("code", 901);
				msgMap.put("message", "没有部署流程");
			} else {
				msgMap.put("code", 902);
				msgMap.put("message", "系统内部错误!");
			}
		} catch (Exception e) {
			msgMap.put("code", 902);
			msgMap.put("message", "系统内部错误!");
		}
		tempMap.put("status", msgMap);
		JSONObject jsonObject = JSONObject.fromObject(tempMap);
		return jsonObject; // 跳转到原来的页面
	}

	/**
	 * 根据用户Id查询待办任务列表
	 * @param userid
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/task/list", method = { RequestMethod.GET })
	public JSONObject findRestTask(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		JSONObject resultJson = new JSONObject();
		// 用户ID
		String userId = request.getParameter("userId");
		int firstResult = Integer.parseInt(request.getParameter("pageNumber"));
		int maxResults = Integer.parseInt(request.getParameter("pageSize"));
		// 业务类型
		String businessType = request.getParameter("businessType");
		int count = leaveWorkFlowService.findRestTaskCount(userId, businessType);
		List<Map<String, Object>> tasklist = leaveWorkFlowService.findRestTask(userId, businessType, firstResult, maxResults);
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("code", 1);
		msgMap.put("message", "查询成功");
		resultJson.put("status", msgMap);
		resultJson.put("count",count);
		resultJson.put("data", tasklist);
		return resultJson;
	}
	
	/**
	 * 根据用户Id数组查询待办任务列表
	 * @param userid
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/task/mult-users/list", method = { RequestMethod.GET })
	public JSONObject findRestTaskByMultUsers(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		JSONObject resultJson = new JSONObject();
		// 用户ID
		//String userId = request.getParameter("userId");
		String users = request.getParameter("users");
		String[] userArr = users.split(",");
		int firstResult = Integer.parseInt(request.getParameter("pageNumber"));
		int maxResults = Integer.parseInt(request.getParameter("pageSize"));
		// 业务类型
		String businessType = request.getParameter("businessType");
		int count = leaveWorkFlowService.findRestTaskCount(userArr, businessType);
		List<Map<String, Object>> tasklist = leaveWorkFlowService.findRestTask(userArr, businessType, firstResult, maxResults);
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("code", 1);
		msgMap.put("message", "查询成功");
		resultJson.put("status", msgMap);
		resultJson.put("count",count);
		resultJson.put("data", tasklist);
		return resultJson;
	}

	/**
	 * 根据任务Id签收任务
	 * 
	 * @param userid
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/task/{taskId}/claim", method = { RequestMethod.GET })
	public JSONObject claimTask(@PathVariable("taskId") String taskId, HttpServletRequest request,
			HttpServletResponse response, HttpSession session) {
		// 返回Json
		JSONObject resultJson = new JSONObject();
		Map<String, Object> tempMap = new HashMap<String, Object>();
		String userId = request.getParameter("userId");
		taskService.claim(taskId, userId);
		tempMap.put("code", 2);
		tempMap.put("message", "任务签收成功!");
		resultJson.put("status", tempMap);
		return resultJson;
	}

	/**
	 * 根据任务Id完成任务
	 * @param userid
	 * @return
	 * @throws IOException 
	 */
	@ResponseBody
	@RequestMapping(value = "/task/{taskId}/complete", method = { RequestMethod.GET, RequestMethod.POST })
	public Map<String, Object> completeTask(@PathVariable("taskId") String taskId, Variables variable) {
		// 返回Json
		JSONObject resultJson = new JSONObject();
		Map<String, Object> tempMap = new HashMap<String, Object>();
		try {
			Map<String, Object> variables = variable.getVariableMap();
			taskService.complete(taskId, variables);
			tempMap.put("code", 3);
			tempMap.put("message", "审批成功!");
		} catch (Exception e) {
			tempMap.put("message", "审核失败!");
			tempMap.put("code", 903);
		}
		resultJson.put("status", tempMap);
		return resultJson;
	}
	
	/**
	 * 根据taskId完成任务,不传递参数
	 * @param taskId
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/task/{taskId}/completeWithoutVariables", method = { RequestMethod.GET, RequestMethod.POST })
	public Map<String,Object> completeTaskWithoutVariables(@PathVariable("taskId") String taskId){
		// 返回Json
		JSONObject resultJson = new JSONObject();
		Map<String, Object> tempMap = new HashMap<String, Object>();
		try {
			taskService.complete(taskId);
			tempMap.put("code", 3);
			tempMap.put("message", "审批成功!");
		} catch (Exception e) {
			tempMap.put("message", "审核失败!");
			tempMap.put("code", 903);
		}
		resultJson.put("status", tempMap);
		return resultJson;
	}

	/**
	 * 根据流程定义的key查询运行中的流程实例
	 * @param processDefinitionKey
	 * //流程定义key
	 * @param request
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/process/running/{businessType}/list", method = { RequestMethod.GET })
	public JSONObject findRunningProcessInstaces(@PathVariable("businessType") String businessType,
			HttpServletRequest request, HttpServletResponse response) {
		List<Map<String, Object>> runningProcessInstaces = leaveWorkFlowService
				.findRunningProcessInstances(businessType);
		JSONObject resultJson = new JSONObject();
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("status", true);
		msgMap.put("message", "查询成功");
		resultJson.put("result", msgMap);
		resultJson.put("data", runningProcessInstaces);
		return resultJson;
	}

	/**
	 * 查询已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/process/finished/{businessType}/list", method = { RequestMethod.GET })
	public JSONObject findFinishedProcessInstaces(@PathVariable("businessType") String businessType,
			HttpServletRequest request, HttpServletResponse response) {
		List<Map<String, Object>> finishedProcessInstaces = leaveWorkFlowService
				.findFinishedProcessInstances(businessType);
		JSONObject jsonObject = new JSONObject();
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("code", 1);
		msgMap.put("message", "查询成功");
		jsonObject.put("status", msgMap);
		jsonObject.put("data", finishedProcessInstaces);
		return jsonObject;
	}

	
	/**
	 * 查询指定用户参与已结束的流程实例
	 * @param processDefinitionKey
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/process/finished/{userId}/historylist", method = { RequestMethod.GET })
	public JSONObject findFinishedProcessInstacesByUser(@PathVariable("userId") String userId,
			HttpServletRequest request, HttpServletResponse response) {
		int firstResult = Integer.parseInt(request.getParameter("pageNumber"));
		int maxResults = Integer.parseInt(request.getParameter("pageSize"));
		String beginTime = request.getParameter("beginTime");
		String endTime   = request.getParameter("endTime");
		System.out.println("beginTime="+beginTime+"111");
		System.out.println("endTime="+endTime+"222");
		String businessType = request.getParameter("businessType");
		List<Map<String, Object>> finishedProcessInstaces = leaveWorkFlowService
				.findFinishedProcessInstancesByUser(userId, 
						firstResult, 
						maxResults,
						beginTime,
						endTime,
						businessType);
		int count = leaveWorkFlowService.
				findFinishedProcessInstancesByUser(userId,
						beginTime,
						endTime,
						businessType);
		JSONObject jsonObject = new JSONObject();
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("code", 1);
		msgMap.put("message", "查询成功");
		jsonObject.put("status", msgMap);
		jsonObject.put("count",count);
		jsonObject.put("data", finishedProcessInstaces);
		return jsonObject;
	}
	
	/**
	 * 查询指定用户参与已结束的流程实例
	 * @param processDefinitionKey
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/process/finished/mult-users/historylist", method = { RequestMethod.GET })
	public JSONObject findFinishedProcessInstacesByUsers(HttpServletRequest request, HttpServletResponse response) {
		String users = request.getParameter("users");
		String[] userArr = users.split(",");
		int firstResult = Integer.parseInt(request.getParameter("pageNumber"));
		int maxResults = Integer.parseInt(request.getParameter("pageSize"));
		String beginTime = request.getParameter("beginTime");
		String endTime   = request.getParameter("endTime");
		System.out.println("beginTime="+beginTime+"111");
		System.out.println("endTime="+endTime+"222");
		String businessType = request.getParameter("businessType");
		List<Map<String, Object>> finishedProcessInstaces = leaveWorkFlowService
				.findFinishedProcessInstancesByUsers(userArr, 
						firstResult, 
						maxResults,
						beginTime,
						endTime,
						businessType);
		int count = leaveWorkFlowService.
				findFinishedProcessInstancesByUsers(userArr,
						beginTime,
						endTime,
						businessType);
		JSONObject jsonObject = new JSONObject();
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("code", 1);
		msgMap.put("message", "查询成功");
		jsonObject.put("status", msgMap);
		jsonObject.put("count",count);
		jsonObject.put("data", finishedProcessInstaces);
		return jsonObject;
	}
	
	/***
	 * 将nodejs中post参数转换成str
	 * @param br
	 * @return
	 */
	public static StringBuffer getRequestContent(HttpServletRequest request)  
            throws IOException {  
        request.setCharacterEncoding("utf8");  
        StringBuffer content = new StringBuffer("");  
          
        String line = null;  
        BufferedReader br = request.getReader();  
        while( (line = br.readLine()) != null){  
            //line = new String(line.getBytes(), "utf-8");  
            content.append(line);   
        }  
        return content;  
    }

}