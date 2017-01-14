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
$IV=$_POST["IV"];
require_once 'MCrypt.php';
$d = new MCrypt;
$d->hex_iv= $IV;
$dUserID = $d->decrypt($UserID);
$response = array();
$d->hex_iv = $d->generateIV(); // the same IV to encrypt all JSON elements
$response["IV"] =$d->hex_iv;
$statement2 = "select TripSummary.TripID from TripSummary where UserID = '".$dUserID."' order by TripID desc limit 1";
$query2 = mysqli_query($con,$statement2);
if (mysqli_num_rows($query2) > 0){
$query22 = mysqli_fetch_array($query2);
$response["MaxTripSummaryID"] = $query22["TripID"];
}else{
$response["MaxTripSummaryID"] = 0;
}
$statement3 = "select TripDetalis.TripID from TripDetalis where UserID = '".$dUserID."' order by TripID desc limit 1";
$query3 = mysqli_query($con,$statement3);
if (mysqli_num_rows($query3) > 0){
$query33 = mysqli_fetch_array($query3);
$response["MaxTripDetailsID"] = $query33["TripID"];
}else{
$response["MaxTripDetailsID"] = 0;
}
echo json_encode($response);
mysqli_close($con)
?>