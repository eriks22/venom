/*
 * Copyright 2018 Preferred.AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.preferred.venom;

import ai.preferred.venom.job.Scheduler;
import ai.preferred.venom.request.Request;
import ai.preferred.venom.response.VResponse;

/**
 * This abstract class represents the method call when the response is makeValidResponse
 * the parsing logic will be up to individual implementations.
 *
 * @author Maksim Tkachenko
 * @author Ween Jiann Lee
 */
public abstract class Handler {
  private Request request;
  private VResponse response;
  private Scheduler scheduler;
  private Session session;
  private Worker worker;

  public Request getRequest() {
    return request;
  }

  public VResponse getResponse() {
    return response;
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  public Session getSession() {
    return session;
  }

  public Worker getWorker() {
    return worker;
  }

  public abstract void tokenize();
  public abstract void parse();
  public abstract void extract();
  public void useStorageHook() {}
  public void continueCrawlHook() {}


  /**
   * This function is called when the request is fetched successfully.
   * <p>
   * This function will hold the logic after the page/file has been fetched.
   * </p>
   *
   * @param request   request fetched.
   * @param response  venom response received.
   * @param scheduler scheduler used for this request.
   * @param session   session variables defined when the crawler is initiated.
   * @param worker    provides the ability to run code in a separate thread.
   */
  void handle(Request request, VResponse response, Scheduler scheduler, Session session, Worker worker) {
    this.request = request;
    this.response = response;
    this.scheduler = scheduler;
    this.session = session;
    this.worker = worker;

    // Provide optional hook to store the parsed data
    // This must be extended in the implementing subclass to add storage capabilities
    useStorageHook();

    // Tokenize the request so that it can be parsed. Required.
    tokenize();

    // Parse the request into a format that can be extracted. Required.
    parse();

    // Extract the parsed data for analysis. Required.
    extract();

    // Provide an optional hook to allow for follow up requests
    // This must be extended in the implementing subclass if follow up requests are wanted
    continueCrawlHook();
  }

}
