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
	

class MCrypt {
     
    private $key = 'U1MjU1M0FDOUZ.Qz';
    public $hex_iv  = ''; 
//===============================================================================

    function __construct() {
        $this->key = hash('sha256', $this->key, true);
    }
//===============================================================================

    function generateIV(){
	$pr_bits = '';

	// Unix/Linux platform?
	$fp = @fopen('/dev/urandom','rb');
	if ($fp !== FALSE) {
    		$pr_bits .= @fread($fp,16);
    		@fclose($fp);
	}

	// MS-Windows platform?
	if (class_exists('COM')) {
		try {
        		$CAPI_Util = new COM('CAPICOM.Utilities.1');
        		$pr_bits .= $CAPI_Util->GetRandom(16,0);
        		if ($pr_bits) { $pr_bits = md5($pr_bits,TRUE); }
    		} catch (Exception $ex) {
			$x =   '00000000000000000000000000000000';
    		}
    		if (strlen($pr_bits) != 16) {
			$x =   '00000000000000000000000000000000';
   		}else{
			$x = $pr_bits;
    		}
    	}else{
		//$x =   '00000000000000000000000000000000';
		//$x= $this->secure_rand(32);
		$x= $this->random_str(32);
	}	
	return  $x;
    }
//===============================================================================
	
    function encrypt($str) {    
	//$this->hex_iv = $this->generateIV();    
        $td = mcrypt_module_open(MCRYPT_RIJNDAEL_128, '', MCRYPT_MODE_CBC, '');
        mcrypt_generic_init($td, $this->key, $this->hexToStr($this->hex_iv));
        $block = mcrypt_get_block_size(MCRYPT_RIJNDAEL_128, MCRYPT_MODE_CBC);
        $pad = $block - (strlen($str) % $block);
        $str .= str_repeat(chr($pad), $pad);
        $encrypted = mcrypt_generic($td, $str);
        mcrypt_generic_deinit($td);
        mcrypt_module_close($td);        
        return base64_encode($encrypted);
    }
//===============================================================================

    function decrypt($code) {     
        $td = mcrypt_module_open(MCRYPT_RIJNDAEL_128, '', MCRYPT_MODE_CBC, '');
        mcrypt_generic_init($td, $this->key, $this->hexToStr($this->hex_iv));
        $str = mdecrypt_generic($td, base64_decode($code));
        $block = mcrypt_get_block_size(MCRYPT_RIJNDAEL_128, MCRYPT_MODE_CBC);
        mcrypt_generic_deinit($td);
        mcrypt_module_close($td);        
        return $this->strippadding($str);               
    }


//===============================================================================

    private function addpadding($string, $blocksize = 16) {
        $len = strlen($string);
        $pad = $blocksize - ($len % $blocksize);
        $string .= str_repeat(chr($pad), $pad);
        return $string;
    }
//===============================================================================

    private function strippadding($string) {
        $slast = ord(substr($string, -1));
        $slastc = chr($slast);
        $pcheck = substr($string, -$slast);
        if (preg_match("/$slastc{" . $slast . "}/", $string)) {
            $string = substr($string, 0, strlen($string) - $slast);
            return $string;
        } else {
            return false;
        }
    }
//===============================================================================

	function hexToStr($hex){
		$string='';
		for ($i=0; $i < strlen($hex)-1; $i+=2){
            $string .= chr(hexdec($hex[$i].$hex[$i+1]));
		}
        return $string;
	}


//===============================================================================

	function random_str(
    	$length,
    	$keyspace = '0A1BC234D56E78F9GHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
	) {
    		$str = '';
    		$max = mb_strlen($keyspace, '8bit') - 1;
    		if ($max < 1) {
        		throw new Exception('$keyspace must be at least two characters long');
    		}
    		for ($i = 0; $i < $length; ++$i) {
        		$str .= $keyspace[rand(0, $max)*15/61];			
		}
    		return $str;
	}
}
?>