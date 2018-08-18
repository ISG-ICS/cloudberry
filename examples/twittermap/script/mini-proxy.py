from flask import Flask,request,Response
import requests as req

app = Flask(__name__)

@app.route("/spoof",methods=["POST","GET"])
def spoof():
    #set user_agent for http request header
    user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36"
    query_word = request.args.get("query")
    url = "https://twitter.com/i/search/typeahead.json?count=10&filters=true&q="+query_word+"&result_type=topics"

    res = req.get(url,headers={"user-agent":user_agent,"referer":"https://twitter.com/"})

    resp = Response(res.content)

    resp.headers['Access-Control-Allow-Origin'] = '*'

    return resp

if __name__ == "__main__":
    app.run()


