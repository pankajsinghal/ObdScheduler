<!doctype html>
 
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>My page</title>
  <link rel="stylesheet" type="text/css" href="newjob.css"/>
  <link rel="stylesheet" type="text/css" href="header.css"/>
</head>
<body>
<div id='cssmenu'>
<ul>
   <li class='has-sub'><a href='#'><span>Scheduler</span></a>
      <ul>
         <li><a href='#'><span>Running Service</span></a></li>
         <li><a href='#'><span>Scheduled Service</span></a></li>
		 <li><a href='#'><span>Ended Service</span></a></li>
		 <li><a href='newjob.html'><span>Add Job</span></a></li>
      </ul>
   </li>
</ul>
</div>
<div id="stylized" class="myform">
  <form id="form" name="form" method="post" action="" onsubmit="return validateForm()">
  <h1>New Service</h1>
     <label>Service Name
 <span class="small">Name</span>
   </label>
 <input type="text" name="sname" id="sname" />
	<label>Job Name
 <span class="small">Job Number</span>
   </label>
 <input type="text" name="jname" id="jname" />
 <label>Job Group
 <span class="small">Group Name</span>
   </label>
 <input type="text" name="gname" id="gname" />
 <label>Start Time
 <span class="small">YYYY-MM-DD HH:MM:SS</span>
   </label>
 <input type="text" name="stime" id="stime" />
 <label>End Time
 <span class="small">YYYY-MM-DD HH:MM:SS</span>
   </label>
 <input type="text" name="etime" id="etime" >
 <label>MSISDN
 <span class="small">Upload Numbers</span>
	</label>
 <input type="file" name="fanme" id="fname" accept="text/*" multiple>
 <label>Priority
 <span class="small">0-10</span>
   </label>
 <select type="text" name="priority" id="priority" >
 <option value="select" selected>-- SELECT --</option>
 <option value="0">0</option>
<option value="1">1</option>
<option value="2">2</option>
<option value="3">3</option>
<option value="4">4</option>
<option value="5">5</option>
<option value="6">6</option>
<option value="7">7</option>
<option value="8">8</option>
<option value="9">9</option>
<option value="10">10</option>
</select>
 
     <button  type="submit">ADD</button>
 <div class="spacer"></div>
 
   </form>
 </div>
 </body>
 </head>