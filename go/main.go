package main

import (
	"fmt"
	"net/http"
	"strconv"
	"log"
)

func main() {
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "Hi from Go! You've requested: %s\n", r.URL.Path)
	})

	port := 9002

	fmt.Print("Web server starting on port " + strconv.Itoa(port) + "\n")

	log.Fatal(http.ListenAndServe(":" + strconv.Itoa(port), nil))
}