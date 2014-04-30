(ns chat-nickname.test.handler
  (:use clojure.test
        ring.mock.request
        chat-nickname.handler))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (> (.indexOf (:body response) "<title>chat-nickname</title>") 0)
          "Title should be 'chat-nickname'")))

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
