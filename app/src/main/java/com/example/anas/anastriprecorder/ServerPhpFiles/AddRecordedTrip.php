<?php
Define ('dbUser','');
Define ('dbHost','');
Define ('dbName','');
Define ('dbPW','');
$con = mysqli_connect(dbHost,dbUser,dbPW);
if(!$con){
	die("db Connection failed :" . mysqli_connect_error($con));
	exit();
}
$dbSelected = mysqli_select_db($con,dbName);
if(!$dbSelected){
	die("db Selection failed :" . mysqli_error($con));
	exit();
}
$UserID = $_POST["UserID"];
$TripID = $_POST["TripID"];
$TripPartID = $_POST["TripPartID"];
$Distance = $_POST["Distance"];
$StartAddress= $_POST["StartAddress"];
$StopAddress = $_POST["StopAddress"];
$StartDate = $_POST["StartDate"];
$StopDate = $_POST["StopDate"];
$StartTime = $_POST["StartTime"];
$StopTime = $_POST["StopTime"];
$StartLatitude = $_POST["StartLatitude"];
$StartLongitude= $_POST["StartLongitude"];
$StopLatitude= $_POST["StopLatitude"];
$StopLongitude= $_POST["StopLongitude"];
$DistanceUnit= $_POST["DistanceUnit"];
$Duration= $_POST["Duration"];
$ManuallyAdded = $_POST["ManuallyAdded"];
$IV=$_POST["IV"];
require_once 'MCrypt.php';
$d = new MCrypt;
$d->hex_iv= $IV;
$dUserID = $d->decrypt($UserID);
$dDistance= $d->decrypt($Distance);
$dStartAddress= $d->decrypt($StartAddress);
$dStopAddress= $d->decrypt($StopAddress );
$dStartDate= $d->decrypt($StartDate );
$dStopDate= $d->decrypt($StopDate );
$dStartTime= $d->decrypt($StartTime);
$dStopTime = $d->decrypt($StopTime );
$dStartLatitude= $d->decrypt($StartLatitude);
$dStartLongitude= $d->decrypt($StartLongitude);
$dStopLatitude= $d->decrypt($StopLatitude);
$dStopLongitude= $d->decrypt($StopLongitude);
$dDistanceUnit = $d->decrypt($DistanceUnit);
$dDuration= $d->decrypt($Duration);
$dManuallyAdded = $d->decrypt($ManuallyAdded);
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
$statement = "INSERT INTO TripDetalis (UserID,TripID,TripPartID,PartStartAddress,PartStartLatitude,PartStartLongitude,PartStartDate,PartStartTime,PartStopAddress,PartStopLatitude,PartStopLongitude,PartStopDate,PartStopTime,TripPartDuration,TripPartDistance,DistanceUnit)Values('".$dUserID."','".(int)$TripID."','".(int)$TripPartID."','".$dStartAddress."','".$dStartLatitude."','".$dStartLongitude."','".$dStartDate."','".$dStartTime."','".$dStopAddress."','".$dStopLatitude."','".$dStopLongitude."','".$dStopDate."','".$dStopTime."','".$dDuration."','".$dDistance."','".$dDistanceUnit."')";
}
$result = mysqli_query($con,$statement) or trigger_error("Query Error".mysqli_error($con));
mysqli_close($con)
?>