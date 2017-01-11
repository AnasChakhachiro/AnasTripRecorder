<?php


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

//$Email= 'aaa@yyy.de';
//$Password = '2222####';
$Email=$_POST["Email"];
$Password= $_POST["Password"];
$IV=$_POST["IV"];
require_once 'MCrypt.php';
//var_dump(get_included_files());
$d = new MCrypt;
$d->hex_iv= $IV;

$dPassword = $d->decrypt($Password);
$dEmail = $d->decrypt($Email);


        $query = array();
	$query = mysqli_query($con,"SELECT * FROM PrivateUserData WHERE Email = '$dEmail' AND Password = '$dPassword' ");

	if (!empty($query)) {
		if (mysqli_num_rows($query) > 0) {
			$query1 = mysqli_fetch_array($query);
			$response = array();
			$d->hex_iv = $d->generateIV(); // the same IV to encrypt all JSON elements 
			$response["IV"] =$d->hex_iv;
			$response["ID"] = $d->encrypt($query1["ID"]);
			$response["Name"] = $d->encrypt($query1["Name"]);
			$response["Email"] = $d->encrypt($query1["Email"]);
			$response["Password"] =$d->encrypt($query1["Password"]);
			$response["RecoveryEmail"] = $d->encrypt($query1["RecoveryEmail"]);
			$response["columnsNumber"] = mysqli_num_fields($query);	
			echo json_encode($response);
		} else {
			$response["success"] = mysqli_num_fields($query);
			$response["message"] = "No user found";
			echo json_encode($response);
		}
	} else {
		$response["success"] = mysqli_num_fields($query);
		$response["message"] = "EmptyQuery";
		echo json_encode($response);
	}
mysqli_close($con)
?>