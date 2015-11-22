<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js"></script>
<link rel="stylesheet"
	href="http://code.jquery.com/ui/1.10.1/themes/base/jquery-ui.css" />
<script src="http://code.jquery.com/jquery-1.9.1.js"></script>
<script src="http://code.jquery.com/ui/1.10.1/jquery-ui.js"></script>	


<div align="left" class="service">
	<h3>Jobs</h3>
</div>
<table cellspacing='0' id="viewservice">
	<thead>
		<tr>
			<th>Job Name</th>
			<th>job group</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${list}" var="userList">
			<tr class="running">
				<%-- <td align="center"><input type="checkbox" class="case" name="case" value='<c:out value="${userList.id}" />'/></td> --%>
				<%-- <td><a href=" #" class="image" title="" data-tooltip ="sticky<c:out value="${theCount.count}" />"><c:out value="${userList.serviceName}" /></a></td> --%>
				<td><c:out value="${userList.jobName}" /></td>
				<td><c:out value="${userList.jobGroup}" /></td>
			</tr>

		</c:forEach>
	</tbody>
</table>

