package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "Hi from Go! You've requested: %s\n", r.URL.Path)
	})

	http.ListenAndServe(":9002", nil)
}