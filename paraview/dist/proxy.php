<?php
    function relayRequest($query){
        $ch = curl_init(); 
        // curl_setopt($ch, CURLOPT_URL, "http://localhost:19002/query/service");
        curl_setopt($ch, CURLOPT_URL, "http://textdb.ics.uci.edu:19002/query/service");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $query);
        $output = curl_exec($ch);
        curl_close($ch);
        
        return $output;
    }
    
    foreach ($_POST as $key => $value){
        relayRequest("$key=$value");
    }
?>
