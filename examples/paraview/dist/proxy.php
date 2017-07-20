<?php
    function relayRequest($query){
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, "http://localhost:19002/query/service"); // URL to asterixDB
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $query);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type:application/x-www-form-urlencoded'));
        $output = curl_exec($ch);
        curl_close($ch);
        
        return $output;
    }
    function registerDDL($ddl){
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, "http://localhost:9000/admin/register"); // URL to Cloudberry registration service
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $ddl);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type:application/json'));
        curl_exec($ch);
        curl_close($ch);
    }
    
    if (isset($_POST['asterixdb'])){
        relayRequest($_POST['asterixdb']);
    }
    
    if (isset($_POST['cloudberry'])){
        registerDDL($_POST['cloudberry']);
    }
?>
