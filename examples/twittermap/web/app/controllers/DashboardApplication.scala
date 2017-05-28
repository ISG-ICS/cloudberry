package controllers

import play.api.mvc.{Action, Controller}

class DashboardApplication extends Controller {

  def index = Action {
    Ok(views.html.dashboard.index("Dashboard"))
  }
}
