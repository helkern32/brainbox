<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js"></script>
<link rel="stylesheet"
	href="http://getskeleton.com/dist/css/skeleton.css">
<link rel="stylesheet"
	href="http://getskeleton.com/dist/css/normalize.css">
<script type="text/javascript">
	// <![CDATA[
	jQuery(document).ready(
			function() {
				jQuery('.spoiler-text').hide()
				jQuery('.spoiler').click(
						function() {
							jQuery(this).toggleClass("folded").toggleClass(
									"unfolded").next().slideToggle()
						})
				$("#vfile").change(function() {
					$('#hfile').val(this.value);
				});
			})
	// ]]>
</script>
<style type="text/css">
.spoiler a {
	font-size: 16px;
	color: #000000;
	border-bottom: #000000 dashed 2px;
	text-decoration: none;
}

.spoiler:hover {
	cursor: pointer;
}

.spoiler-text {
	margin: 15px 25px;
	font-size: 18px;
}

h2 {
	font-family: "Comic Sans MS";
	font-size: 34px;
	font-style: normal;
	font-variant: normal;
	font-weight: 500;
	line-height: 26.4px;
}

.spoiler-wrapper,a {
	font-family: "Comic Sans MS";
	font-size: 18px;
	font-style: normal;
	font-variant: normal;
	font-weight: 500;
	line-height: 26.4px;
}

body {
	margin-top: 30px;
	margin-left: 20px;
}

.custom-file-input::-webkit-file-upload-button {
	visibility: hidden;
}

.custom-file-input::before {
	content: 'Select files';
	display: inline-block;
	background: -webkit-linear-gradient(top, #f9f9f9, #e3e3e3);
	border: 1px solid #999;
	border-radius: 3px;
	padding: 5px 8px;
	outline: none;
	white-space: nowrap;
	-webkit-user-select: none;
	cursor: pointer;
	text-shadow: 1px 1px #fff;
	font-weight: 700;
	font-size: 10pt;
}

.custom-file-input:hover::before {
	border-color: black;
}

.custom-file-input:active::before {
	background: -webkit-linear-gradient(top, #e3e3e3, #f9f9f9);
}
</style>
<title>Java docs</title>
</head>
<body bgcolor="#E6E6FA">
	<h2>${directory}</h2>
	<c:choose>
		<c:when test="${showUploadLink == false}">
			<div class="spoiler-wrapper">
				<div class="spoiler folded">
					<a href="javascript:void(0);">Create new folder</a>
				</div>
				<div class="spoiler-text">
					<form action="./createdir.html" method="POST">
						Folder name:<br> <input type="text" name="foldername" value=""> 
						&#160;<input class="button-primary" type="submit" value="Submit">
					</form>
				</div>
			</div>
		</c:when>
		<c:otherwise>
			<div class="spoiler-wrapper">
				<div class="spoiler folded">
					<a href="javascript:void(0);">Upload file</a>
				</div>
				<div class="spoiler-text">
					<form action="./${directory}/upload.html" method="POST" enctype="multipart/form-data">
						<input type="text" name="fname" id="hfile" hidden> 
						Select File to Upload: <input type="file" name="vname" id="vfile" value="Select file" class="custom-file-input" > 
						<input class="button-primary" type="submit" value="Upload" style="font-weight: bold;">
					</form>
				</div>
			</div>
		</c:otherwise>
	</c:choose>
	<br />
	<form action="./delete.html" method="POST">
		<c:forEach items="${list}" var="item">
			<p>
				<input type="checkbox" name="${item.href}"> <a href="<c:out  value='${item.href}' />">${item.name}</a>
			</p>
		</c:forEach>
		<br /> <br /> <br />
		<c:choose>
			<c:when test="${showUploadLink == true}">
				<input type="email" name="email" value="sergii.rubinov@kindle.com">
				<button class="button-primary" type="submit" value="Send" formaction="./send.html">Send on kindle</button>
				<button class="button-primary" type="submit" value="Unzip" formaction="./unzip.html">Unzip</button>
			</c:when>
		</c:choose>
		<button class="button-primary" type="submit" value="Delete" onClick="return validate()">Delete items</button>
	</form>
	<c:choose>
		<c:when test="${showUploadLink == true}">
			<h3>
				<a href="<c:out value='${back}' />">Back</a>
			</h3>
		</c:when>
	</c:choose>
	<script type="text/javascript">
		function validate() {
			password = "";
			password = prompt("Please, enter password", "");
			if (password == '${pass}') {
				return true;
			} else {
				alert("Password incorrect!");
				return false;
			}
		}
	</script>
</body>
</html>