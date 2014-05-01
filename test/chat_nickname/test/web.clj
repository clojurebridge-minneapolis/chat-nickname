(ns chat-nickname.test.web
  (:use clojure.test
        ring.mock.request
        chat-nickname.web))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (> (.indexOf (:body response) "<title>chat") 0)
          "Title should start with chat-")))

  (testing "hello"
    (let [response (app (request :get "/hello"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World!"))))

  (testing "resources"
    (let [response (app (request :get "/css/chat.css"))]
      (is (= (:status response) 200))
      (is (= (get (:headers response) "Content-Type") "text/css"))
      (is (= (class (:body response)) java.io.File))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
