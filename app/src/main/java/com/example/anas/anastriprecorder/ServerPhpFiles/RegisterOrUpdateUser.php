<?php

Define ('dbUser','');
Define ('dbHost','');
Define ('dbName','');
Define ('dbPW', '');

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

$ID = (int)$_POST["ID"];
$IDstring =$_POST["ID"];
$Name = $_POST["Name"];
$Email = $_POST["Email"];
$Password = $_POST["Password"];
$RecoveryEmail = $_POST["RecoveryEmail"];
$Purpose = $_POST["Purpose"];

$IV=$_POST["IV"];
require_once 'MCrypt.php';
//var_dump(get_included_files());
$d = new MCrypt;
$d->hex_iv= $IV;

$dName = $d->decrypt($Name);
$dEmail = $d->decrypt($Email);
$dPassword = $d->decrypt($Password);
$dRecoveryEmail = $d->decrypt($RecoveryEmail);
$dPurpose = $d->decrypt($Purpose);
$dIDstring = $d->decrypt($IDstring);

//$dName = "Deema";
//$dEmail = "dydr@ee.com";
//$dPassword = "3333####";
//$dRecoveryEmail = "dd@eew.de" ;
//$dPurpose = "Register";
//$dIDstring = "";

if($dPurpose == "Register"){	
	$statement = "INSERT INTO PrivateUserData (Name,Email,Password,RecoveryEmail) Values('".$dName."','".$dEmail."','".$dPassword."','".$dRecoveryEmail."')";
}
if($dPurpose == "Update"){
	$statement="UPDATE PrivateUserData SET Name ='".$dName."' , Email = '".$dEmail."' ,Password ='".$dPassword."' , RecoveryEmail = '".$dRecoveryEmail."' WHERE ID = '".$dIDstring."'  ";
}
$result = mysqli_query($con,$statement) or trigger_error("Query Error".mysqli_error($con));
mysqli_close($con)


?>